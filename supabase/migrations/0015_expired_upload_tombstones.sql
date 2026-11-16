-- Fail-closed tombstones for expired pending uploads. Retain asset ledger and
-- all attempt counts; no quota is reopened and no derivative is deleted.
alter table temple_private.photo_assets add column abandoned_at timestamptz;
alter table temple_private.photo_assets add column abandoned_absence_checked_at timestamptz;
create or replace function public.can_upload_temple_photo(p_bucket text,p_name text) returns boolean
language sql stable security definer set search_path='' as $$
 select p_bucket='temple-photo-quarantine' and public.is_admin()
 and exists(select 1 from temple_private.photo_config where id=true and enabled=true)
 and exists(select 1 from temple_private.photo_assets where created_by=auth.uid()
  and original_path=p_name and state='pending' and abandoned_at is null and expires_at>clock_timestamp())
$$;
create function public.abandon_expired_temple_photo_uploads() returns jsonb
language plpgsql security definer set search_path='' as $$
begin
 -- Same lock order as claims: config then asset. Cleanup remains possible when
 -- new uploads are disabled. A one-day grace leaves ordinary in-flight uploads
 -- alone. This cannot cancel an external Storage request already accepted.
 perform 1 from temple_private.photo_config where id=true for update;
 update temple_private.photo_assets set abandoned_at=clock_timestamp(),processing_token=null,processing_until=null
 where state='pending' and abandoned_at is null and expires_at<clock_timestamp()-interval '1 day'
  and (processing_until is null or processing_until<clock_timestamp()-interval '1 day');
 -- Always return tombstoned rows, even after an absence check. A late external
 -- upload could land after a prior delete. Periodic reconciliation must repeat.
 return (select coalesce(jsonb_agg(jsonb_build_object('id',id,'bucket','temple-photo-quarantine','path',original_path)),'[]'::jsonb)
  from temple_private.photo_assets where abandoned_at is not null and state='pending');
end $$;
create function public.confirm_abandoned_temple_photo_absent(p_id uuid) returns void
language plpgsql security definer set search_path='' as $$
declare p text;
begin
 select original_path into p from temple_private.photo_assets where id=p_id and state='pending' and abandoned_at is not null for update;
 if not found then raise exception 'photo_not_abandoned' using errcode='22023'; end if;
 if exists(select 1 from storage.objects where bucket_id='temple-photo-quarantine' and name=p) then
  raise exception 'original_still_exists' using errcode='22023'; end if;
 -- Point-in-time metadata absence, NOT proof a previously accepted request can
 -- never land later, and NOT a permanent erasure claim.
 update temple_private.photo_assets set abandoned_absence_checked_at=clock_timestamp() where id=p_id;
end $$;
revoke all on function public.abandon_expired_temple_photo_uploads(),public.confirm_abandoned_temple_photo_absent(uuid) from public,anon,authenticated,service_role;
grant execute on function public.abandon_expired_temple_photo_uploads(),public.confirm_abandoned_temple_photo_absent(uuid) to service_role;

create or replace function public.claim_private_temple_photo(p_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_token uuid:=gen_random_uuid(); a temple_private.photo_assets%rowtype;
begin
 perform 1 from temple_private.photo_config where id=true and enabled=true for update;
 if not found then return null; end if;
 update temple_private.photo_assets set processing_token=v_token,processing_attempts=processing_attempts+1,
   processing_until=clock_timestamp()+interval '2 minutes'
 where id=p_id and state='pending' and abandoned_at is null and expires_at>clock_timestamp() and processing_attempts<2
   and (processing_until is null or processing_until<clock_timestamp())
 returning * into a;
 if not found then return null; end if;
 return jsonb_build_object('id',a.id,'bucket','temple-photo-quarantine','path',a.original_path,
   'lease_token',v_token,'output_path','processed/'||a.id::text||'/'||v_token::text||'.jpg');
end $$;
