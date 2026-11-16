-- Durable publication intent only. No public bucket, serving policy, gallery
-- mutation or verification change. Real publication needs a separate gated
-- worker/finalization stage. Existing visitors keep their legacy gallery.
create table temple_private.photo_publication_config (
 id boolean primary key default true check(id), enabled boolean not null default false
);
insert into temple_private.photo_publication_config(id) values(true);
create table temple_private.photo_publication_requests (
 id uuid primary key,
 asset_id uuid not null references temple_private.photo_assets(id),
 requested_by uuid not null references auth.users(id),
 requested_at timestamptz not null default clock_timestamp(),
 expected_revision bigint not null check(expected_revision>=0),
 caption_ml text not null check(length(caption_ml)<=2000),
 caption_en text not null check(length(caption_en)<=2000),
 state text not null default 'queued' check(state in ('queued','canceled')),
 canceled_at timestamptz,
 unique(asset_id,expected_revision)
);
alter table temple_private.photo_publication_config enable row level security;
alter table temple_private.photo_publication_requests enable row level security;
revoke all on temple_private.photo_publication_config,temple_private.photo_publication_requests from public,anon,authenticated,service_role;
create function public.queue_reviewed_temple_photo(p_request_id uuid,p_asset_id uuid,p_expected_revision bigint,p_caption_ml text,p_caption_en text) returns jsonb
language plpgsql security definer set search_path='' as $$
declare r temple_private.photo_publication_requests%rowtype; v_revision bigint;
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 perform 1 from temple_private.photo_publication_config where id=true and enabled=true for update;
 if not found then raise exception 'photo_publication_disabled' using errcode='42501'; end if;
 if p_request_id is null or p_asset_id is null or p_expected_revision is null or p_expected_revision<0
  or p_caption_ml is null or p_caption_en is null or length(p_caption_ml)>2000 or length(p_caption_en)>2000 then
  raise exception 'invalid_publication_request' using errcode='22023'; end if;
 -- Retry returns the same intent only, never interprets a changed payload as an
 -- approval of new words. A retry after cancellation stays canceled.
 select * into r from temple_private.photo_publication_requests where id=p_request_id;
 if found then
  if r.asset_id<>p_asset_id or r.requested_by<>auth.uid() or r.expected_revision<>p_expected_revision
   or r.caption_ml<>p_caption_ml or r.caption_en<>p_caption_en then
   raise exception 'publication_idempotency_conflict' using errcode='22023'; end if;
  return jsonb_build_object('id',r.id,'state',r.state);
 end if;
 select revision into v_revision from public.content_meta where section='gallery' for update;
 if v_revision is null or v_revision<>p_expected_revision then raise exception 'gallery_revision_conflict' using errcode='40001'; end if;
 perform 1 from temple_private.photo_assets a where a.id=p_asset_id and a.state='reviewed'
  and exists(select 1 from storage.objects o where o.bucket_id='temple-photo-quarantine' and o.name=a.normalized_path) for update of a;
 if not found then raise exception 'reviewed_photo_required' using errcode='22023'; end if;
 -- Keep queued intent bounded independently of gallery revisions. Canceled
 -- intents remain as idempotency/audit records; no automatic deletion.
 if (select count(*) from temple_private.photo_publication_requests where state='queued')>=24 then
  raise exception 'publication_queue_full' using errcode='54000'; end if;
 insert into temple_private.photo_publication_requests(id,asset_id,requested_by,expected_revision,caption_ml,caption_en)
 values(p_request_id,p_asset_id,auth.uid(),p_expected_revision,p_caption_ml,p_caption_en);
 return jsonb_build_object('id',p_request_id,'state','queued');
end $$;
create function public.cancel_temple_photo_publication(p_request_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare r temple_private.photo_publication_requests%rowtype;
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 -- Cancellation works even when new publication requests are disabled.
 perform 1 from temple_private.photo_publication_config where id=true for update;
 update temple_private.photo_publication_requests set state='canceled',canceled_at=coalesce(canceled_at,clock_timestamp())
 where id=p_request_id returning * into r;
 if not found then raise exception 'publication_request_missing' using errcode='22023'; end if;
 return jsonb_build_object('id',r.id,'state',r.state);
end $$;
create function public.list_temple_photo_publication_requests() returns jsonb
language plpgsql stable security definer set search_path='' as $$
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 return (select coalesce(jsonb_agg(jsonb_build_object('id',id,'asset_id',asset_id,'state',state,
 'requested_at',requested_at,'expected_revision',expected_revision,'caption_ml',caption_ml,'caption_en',caption_en)
 order by requested_at desc),'[]'::jsonb) from temple_private.photo_publication_requests);
end $$;
revoke all on function public.queue_reviewed_temple_photo(uuid,uuid,bigint,text,text),public.cancel_temple_photo_publication(uuid),public.list_temple_photo_publication_requests() from public,anon,authenticated;
grant execute on function public.queue_reviewed_temple_photo(uuid,uuid,bigint,text,text),public.cancel_temple_photo_publication(uuid),public.list_temple_photo_publication_requests() to authenticated;
