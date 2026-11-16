-- Private derivative staging and atomic publication manifest. Unapplied;
-- both request/delivery flags default false. No public bucket is created.
alter table temple_private.photo_publication_config add column delivery_enabled boolean not null default false;
alter table temple_private.photo_publication_requests drop constraint photo_publication_requests_state_check;
alter table temple_private.photo_publication_requests add constraint photo_publication_requests_state_check check(state in ('queued','canceled','published'));
-- Conservative storage ceiling: one intent and at most two output objects per
-- asset over its lifetime. Canceled/stale requests do not reopen capacity.
alter table temple_private.photo_publication_requests add constraint photo_publication_asset_once unique(asset_id);
alter table temple_private.photo_publication_requests add column attempts integer not null default 0 check(attempts between 0 and 2);
alter table temple_private.photo_publication_requests add column lease_token uuid;
alter table temple_private.photo_publication_requests add column lease_until timestamptz;
alter table temple_private.photo_publication_requests add column claimed_source_path text;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('temple-photo-gallery','temple-photo-gallery',false,4194304,array['image/jpeg']);
-- Intentionally NO storage SELECT policy, even for anonymous users/admins.
-- A narrow server gateway reads only a current finalized manifest, never an
-- original, arbitrary path or signed URL. Bucket/object direct links stay private.
create table temple_private.photo_gallery_manifest (
 publication_id uuid primary key references temple_private.photo_publication_requests(id),
 asset_id uuid unique not null references temple_private.photo_assets(id),
 object_path text unique not null,
 caption_ml text not null, caption_en text not null,
 published_at timestamptz not null default clock_timestamp()
);
alter table temple_private.photo_gallery_manifest enable row level security;
revoke all on temple_private.photo_gallery_manifest from public,anon,authenticated,service_role;
create function public.claim_temple_photo_publication(p_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare r temple_private.photo_publication_requests%rowtype; a temple_private.photo_assets%rowtype; t uuid:=gen_random_uuid(); rev bigint;
begin
 perform 1 from temple_private.photo_publication_config where id=true and enabled and delivery_enabled for update;
 if not found then return null; end if;
 select * into r from temple_private.photo_publication_requests where id=p_id for update;
 if not found or r.state<>'queued' or r.attempts>=2 or r.lease_until>clock_timestamp() then return null; end if;
 select revision into rev from public.content_meta where section='gallery' for update;
 if rev is distinct from r.expected_revision then return null; end if;
 select * into a from temple_private.photo_assets where id=r.asset_id and state='reviewed' for update;
 if not found or not exists(select 1 from public.admin_users where user_id=r.requested_by)
  or not exists(select 1 from public.admin_users where user_id=a.reviewed_by)
  or not exists(select 1 from storage.objects where bucket_id='temple-photo-quarantine' and name=a.normalized_path) then return null; end if;
 update temple_private.photo_publication_requests set attempts=attempts+1,lease_token=t,
  lease_until=clock_timestamp()+interval '2 minutes',claimed_source_path=a.normalized_path where id=p_id;
 return jsonb_build_object('id',r.id,'asset_id',a.id,'lease_token',t,'source_bucket','temple-photo-quarantine',
  'source_path',a.normalized_path,'target_bucket','temple-photo-gallery','target_path',a.id::text||'/'||r.id::text||'/'||t::text||'.jpg');
end $$;
create function public.finish_temple_photo_publication(p_id uuid,p_token uuid) returns bigint
language plpgsql security definer set search_path='' as $$
declare r temple_private.photo_publication_requests%rowtype; a temple_private.photo_assets%rowtype; rev bigint; target text;
begin
 perform 1 from temple_private.photo_publication_config where id=true and enabled and delivery_enabled for update;
 if not found then raise exception 'photo_delivery_disabled' using errcode='42501'; end if;
 select * into r from temple_private.photo_publication_requests where id=p_id for update;
 if not found or r.state<>'queued' or r.lease_token is distinct from p_token or r.lease_until<=clock_timestamp() or r.lease_until is null then
  raise exception 'publication_lease_lost' using errcode='40001'; end if;
 select revision into rev from public.content_meta where section='gallery' for update;
 if rev is distinct from r.expected_revision then raise exception 'gallery_revision_conflict' using errcode='40001'; end if;
 select * into a from temple_private.photo_assets where id=r.asset_id and state='reviewed' and normalized_path=r.claimed_source_path for update;
 if not found or not exists(select 1 from public.admin_users where user_id=r.requested_by)
  or not exists(select 1 from public.admin_users where user_id=a.reviewed_by) then
  raise exception 'publication_authority_changed' using errcode='42501'; end if;
 target:=a.id::text||'/'||r.id::text||'/'||p_token::text||'.jpg';
 if not exists(select 1 from storage.objects where bucket_id='temple-photo-gallery' and name=target) then
  raise exception 'publication_object_missing' using errcode='22023'; end if;
 -- Staging alone never exposes bytes. Only this transaction creates the
 -- gateway-readable manifest and resets content verification with the revision.
 insert into temple_private.photo_gallery_manifest(publication_id,asset_id,object_path,caption_ml,caption_en)
 values(r.id,a.id,target,r.caption_ml,r.caption_en);
 update temple_private.photo_publication_requests set state='published',lease_token=null,lease_until=null where id=r.id;
 update public.content_meta set revision=revision+1,verified=false,verified_at=null,verified_by=null where section='gallery' returning revision into rev;
 return rev;
end $$;
create or replace function public.cancel_temple_photo_publication(p_request_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare r temple_private.photo_publication_requests%rowtype;
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 perform 1 from temple_private.photo_publication_config where id=true for update;
 select * into r from temple_private.photo_publication_requests where id=p_request_id for update;
 if not found then raise exception 'publication_request_missing' using errcode='22023'; end if;
 if r.state='published' then raise exception 'photo_already_published' using errcode='22023'; end if;
 update temple_private.photo_publication_requests set state='canceled',canceled_at=coalesce(canceled_at,clock_timestamp()),lease_token=null,lease_until=null where id=p_request_id;
 return jsonb_build_object('id',p_request_id,'state','canceled');
end $$;
create function public.get_uploaded_temple_gallery() returns jsonb
language sql stable security definer set search_path='' as $$
 select coalesce(jsonb_agg(jsonb_build_object('asset_id',m.asset_id,'publication_id',m.publication_id,
  'caption_ml',m.caption_ml,'caption_en',m.caption_en) order by m.published_at),'[]'::jsonb)
 from temple_private.photo_gallery_manifest m join temple_private.photo_publication_requests r on r.id=m.publication_id
 where r.state='published' and exists(select 1 from temple_private.photo_publication_config where id=true and delivery_enabled)
$$;
create function public.get_temple_gallery_photo_manifest(p_asset_id uuid,p_publication_id uuid) returns jsonb
language sql stable security definer set search_path='' as $$
 select jsonb_build_object('asset_id',m.asset_id,'publication_id',m.publication_id,'bucket','temple-photo-gallery','path',m.object_path)
 from temple_private.photo_gallery_manifest m join temple_private.photo_publication_requests r on r.id=m.publication_id
 where m.asset_id=p_asset_id and m.publication_id=p_publication_id and r.state='published'
  and exists(select 1 from temple_private.photo_publication_config where id=true and delivery_enabled)
$$;
revoke all on function public.claim_temple_photo_publication(uuid),public.finish_temple_photo_publication(uuid,uuid),public.get_temple_gallery_photo_manifest(uuid,uuid),public.get_uploaded_temple_gallery() from public,anon,authenticated,service_role;
grant execute on function public.claim_temple_photo_publication(uuid),public.finish_temple_photo_publication(uuid,uuid),public.get_temple_gallery_photo_manifest(uuid,uuid) to service_role;
grant execute on function public.get_uploaded_temple_gallery() to anon,authenticated,service_role;
