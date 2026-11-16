-- Private-original retention cleanup only, no asset/publication deletion.
-- Asset rows remain counted toward capacity until a separate audited archival
-- path exists. Never delete rows to reopen capacity while files remain.
alter table temple_private.photo_assets add column original_erased_at timestamptz;
create function public.private_photo_cleanup_batch() returns jsonb
language sql stable security definer set search_path='' as $$
 select coalesce(jsonb_agg(jsonb_build_object('id',id,'bucket','temple-photo-quarantine',
   'path',original_path)),'[]'::jsonb)
 from (select id,original_path from temple_private.photo_assets
   where state in ('processed','reviewed') and original_erased_at is null
   order by created_at limit 24) a
$$;
create function public.confirm_private_photo_original_erased(p_id uuid) returns void
language plpgsql security definer set search_path='' as $$
declare v_path text;
begin
 select original_path into v_path from temple_private.photo_assets
   where id=p_id and state in ('processed','reviewed') for update;
 if not found then raise exception 'photo_not_processed' using errcode='22023'; end if;
 -- Storage deletion must go through Storage API, never SQL metadata deletion.
 if exists(select 1 from storage.objects where bucket_id='temple-photo-quarantine' and name=v_path) then
   raise exception 'original_still_exists' using errcode='22023';
 end if;
 update temple_private.photo_assets set original_erased_at=coalesce(original_erased_at,clock_timestamp()) where id=p_id;
end $$;
revoke all on function public.private_photo_cleanup_batch(),public.confirm_private_photo_original_erased(uuid) from public,anon,authenticated;
grant execute on function public.private_photo_cleanup_batch(),public.confirm_private_photo_original_erased(uuid) to service_role;
