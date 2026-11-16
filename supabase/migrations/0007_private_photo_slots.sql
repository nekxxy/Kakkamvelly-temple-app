-- Additive, private photo management. No existing gallery rows/URLs are changed.
-- No public object serving, publication function, or app upload entry point.
create table temple_private.photo_config (
  id boolean primary key default true check(id), enabled boolean not null default false
);
insert into temple_private.photo_config(id) values(true);
create table temple_private.photo_assets (
  id uuid primary key default gen_random_uuid(),
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default clock_timestamp(),
  expires_at timestamptz not null default (clock_timestamp()+interval '15 minutes'),
  state text not null default 'pending' check(state in ('pending','processed','reviewed')),
  original_path text unique not null,
  normalized_path text unique,
  reviewed_by uuid references auth.users(id),
  reviewed_at timestamptz,
  constraint normalized_for_processed check(state='pending' or normalized_path is not null),
  constraint review_attribution check(state<>'reviewed' or (reviewed_by is not null and reviewed_at is not null))
);
alter table temple_private.photo_config enable row level security;
alter table temple_private.photo_assets enable row level security;
revoke all on temple_private.photo_config, temple_private.photo_assets from public, anon, authenticated, service_role;
-- Dedicated bucket only. Fail migration on a pre-existing bucket rather than
-- adopting someone else's contents/policies or silently changing its audience.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('temple-photo-quarantine','temple-photo-quarantine',false,8388608,array['image/jpeg','image/png','image/webp']);

create function public.create_photo_upload_slot() returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_id uuid:=gen_random_uuid(); v_path text;
begin
  if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
  perform 1 from temple_private.photo_config where id=true and enabled=true for update;
  if not found then raise exception 'photo_upload_disabled' using errcode='42501'; end if;
  -- Fixed global pending-slot ceiling bounds outstanding capabilities. Byte
  -- quotas/cleanup and processed storage quotas must also precede activation.
  if (select count(*) from temple_private.photo_assets where state='pending')>=24 then
    raise exception 'photo_slots_full' using errcode='54000';
  end if;
  v_path:=auth.uid()::text||'/'||v_id::text||'/original';
  insert into temple_private.photo_assets(id,created_by,original_path) values(v_id,auth.uid(),v_path);
  return jsonb_build_object('id',v_id,'bucket','temple-photo-quarantine','path',v_path);
end $$;

create function public.can_upload_temple_photo(p_bucket text,p_name text) returns boolean
language sql stable security definer set search_path='' as $$
select p_bucket='temple-photo-quarantine' and public.is_admin()
 and exists(select 1 from temple_private.photo_config where id=true and enabled=true)
 and exists(select 1 from temple_private.photo_assets where created_by=auth.uid()
   and original_path=p_name and state='pending' and expires_at>clock_timestamp())
$$;
create policy "temple private original insert" on storage.objects for insert to authenticated
with check(public.can_upload_temple_photo(bucket_id,name));
-- No SELECT/UPDATE/DELETE policy: originals cannot be publicly viewed or
-- overwritten. A server worker alone reads/re-encodes them. No signed URL RPC.
create function public.finish_temple_photo_processing(p_id uuid,p_normalized_path text) returns void
language plpgsql security definer set search_path='' as $$
begin
  if p_normalized_path is distinct from 'processed/'||p_id::text||'.jpg' then
    raise exception 'invalid_normalized_path' using errcode='22023';
  end if;
  if not exists(select 1 from storage.objects where bucket_id='temple-photo-quarantine' and name=p_normalized_path) then
    raise exception 'normalized_object_missing' using errcode='22023';
  end if;
  -- This service-only assertion MUST follow successful isolated raster decode,
  -- bounds checks, re-encoding and metadata removal. Presence is not validation.
  update temple_private.photo_assets set state='processed',normalized_path=p_normalized_path where id=p_id and state='pending';
  if not found then raise exception 'photo_not_pending' using errcode='22023'; end if;
end $$;
create function public.review_temple_photo(p_id uuid) returns void
language plpgsql security definer set search_path='' as $$
begin
  if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
  update temple_private.photo_assets set state='reviewed',reviewed_by=auth.uid(),reviewed_at=clock_timestamp() where id=p_id and state='processed';
  if not found then raise exception 'photo_not_processed' using errcode='22023'; end if;
  -- Reviewed is NOT published. Existing gallery/content_meta is untouched.
end $$;
revoke all on function public.create_photo_upload_slot(),public.can_upload_temple_photo(text,text),public.review_temple_photo(uuid),public.finish_temple_photo_processing(uuid,text) from public,anon,authenticated;
grant execute on function public.create_photo_upload_slot(),public.can_upload_temple_photo(text,text),public.review_temple_photo(uuid) to authenticated;
grant execute on function public.finish_temple_photo_processing(uuid,text) to service_role;
