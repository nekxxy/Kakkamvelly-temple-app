-- Default-off production preparation: per-photo retirement and caption edits.
-- No object is deleted and no slot/attempt allowance is reclaimed.
alter table temple_private.photo_gallery_manifest add column retired_at timestamptz;
alter table temple_private.photo_gallery_manifest add column retired_by uuid references auth.users(id);
create table temple_private.photo_gallery_changes (
 id bigint generated always as identity primary key,
 publication_id uuid not null references temple_private.photo_gallery_manifest(publication_id),
 changed_by uuid not null references auth.users(id),changed_at timestamptz not null default clock_timestamp(),
 action text not null check(action in ('captions','retire')),
 caption_ml text not null,caption_en text not null,
 revision bigint not null
);
alter table temple_private.photo_gallery_changes enable row level security;
revoke all on temple_private.photo_gallery_changes from public,anon,authenticated,service_role;
create function public.edit_temple_photo_captions(p_id uuid,p_expected_revision bigint,p_caption_ml text,p_caption_en text) returns bigint
language plpgsql security definer set search_path='' as $$
declare rev bigint; m temple_private.photo_gallery_manifest%rowtype;
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 -- A caption edit is new content, so the request feature must still be enabled.
 perform 1 from temple_private.photo_publication_config where id=true and enabled for update;
 if not found then raise exception 'photo_publication_disabled' using errcode='42501'; end if;
 if p_expected_revision is null or p_caption_ml is null or p_caption_en is null or length(p_caption_ml)>2000 or length(p_caption_en)>2000 then
  raise exception 'invalid_photo_captions' using errcode='22023'; end if;
 select revision into rev from public.content_meta where section='gallery' for update;
 if rev is distinct from p_expected_revision then raise exception 'gallery_revision_conflict' using errcode='40001'; end if;
 select * into m from temple_private.photo_gallery_manifest where publication_id=p_id and retired_at is null for update;
 if not found then raise exception 'published_photo_missing' using errcode='22023'; end if;
 if m.caption_ml=p_caption_ml and m.caption_en=p_caption_en then return rev; end if;
 -- Fixed growth cap per photo, not a product entitlement. Keep the audit history.
 if (select count(*) from temple_private.photo_gallery_changes where publication_id=p_id and action='captions')>=50 then
  raise exception 'photo_edit_limit' using errcode='54000'; end if;
 update temple_private.photo_gallery_manifest set caption_ml=p_caption_ml,caption_en=p_caption_en where publication_id=p_id;
 update public.content_meta set revision=revision+1,verified=false,verified_at=null,verified_by=null where section='gallery' returning revision into rev;
 insert into temple_private.photo_gallery_changes(publication_id,changed_by,action,caption_ml,caption_en,revision)
 values(p_id,auth.uid(),'captions',p_caption_ml,p_caption_en,rev);
 return rev;
end $$;
create function public.retire_temple_gallery_photo(p_id uuid,p_expected_revision bigint) returns bigint
language plpgsql security definer set search_path='' as $$
declare rev bigint; m temple_private.photo_gallery_manifest%rowtype;
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 -- Removal remains possible with delivery and new requests disabled.
 perform 1 from temple_private.photo_publication_config where id=true for update;
 select revision into rev from public.content_meta where section='gallery' for update;
 if rev is null then raise exception 'unknown_gallery' using errcode='22023'; end if;
 select * into m from temple_private.photo_gallery_manifest where publication_id=p_id for update;
 if not found then raise exception 'published_photo_missing' using errcode='22023'; end if;
 -- Monotonic/idempotent retirement, never a reactivation route.
 if m.retired_at is not null then return rev; end if;
 if rev is distinct from p_expected_revision then raise exception 'gallery_revision_conflict' using errcode='40001'; end if;
 update temple_private.photo_gallery_manifest set retired_at=clock_timestamp(),retired_by=auth.uid() where publication_id=p_id;
 update public.content_meta set revision=revision+1,verified=false,verified_at=null,verified_by=null where section='gallery' returning revision into rev;
 insert into temple_private.photo_gallery_changes(publication_id,changed_by,action,caption_ml,caption_en,revision)
 values(p_id,auth.uid(),'retire',m.caption_ml,m.caption_en,rev);
 return rev;
end $$;
create or replace function public.get_uploaded_temple_gallery() returns jsonb
language sql stable security definer set search_path='' as $$
 select coalesce(jsonb_agg(jsonb_build_object('asset_id',m.asset_id,'publication_id',m.publication_id,
  'caption_ml',m.caption_ml,'caption_en',m.caption_en) order by m.published_at),'[]'::jsonb)
 from temple_private.photo_gallery_manifest m join temple_private.photo_publication_requests r on r.id=m.publication_id
 where r.state='published' and m.retired_at is null
  and exists(select 1 from temple_private.photo_publication_config where id=true and delivery_enabled)
$$;
create or replace function public.get_temple_gallery_photo_manifest(p_asset_id uuid,p_publication_id uuid) returns jsonb
language sql stable security definer set search_path='' as $$
 select jsonb_build_object('asset_id',m.asset_id,'publication_id',m.publication_id,'bucket','temple-photo-gallery','path',m.object_path)
 from temple_private.photo_gallery_manifest m join temple_private.photo_publication_requests r on r.id=m.publication_id
 where m.asset_id=p_asset_id and m.publication_id=p_publication_id and r.state='published' and m.retired_at is null
  and exists(select 1 from temple_private.photo_publication_config where id=true and delivery_enabled)
$$;
revoke all on function public.edit_temple_photo_captions(uuid,bigint,text,text),public.retire_temple_gallery_photo(uuid,bigint) from public,anon,authenticated,service_role;
grant execute on function public.edit_temple_photo_captions(uuid,bigint,text,text),public.retire_temple_gallery_photo(uuid,bigint) to authenticated;
