-- Service-only fenced processing. Prevent concurrent workers from asserting the
-- same asset, and reject late completion after lease expiry. No activation.
alter table temple_private.photo_assets add column processing_attempts integer not null default 0 check(processing_attempts between 0 and 2);
alter table temple_private.photo_assets add column processing_token uuid;
alter table temple_private.photo_assets add column processing_until timestamptz;
create function public.claim_private_temple_photo(p_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare v_token uuid:=gen_random_uuid(); a temple_private.photo_assets%rowtype;
begin
 perform 1 from temple_private.photo_config where id=true and enabled=true for update;
 if not found then return null; end if;
 update temple_private.photo_assets set processing_token=v_token,processing_attempts=processing_attempts+1,
   processing_until=clock_timestamp()+interval '2 minutes'
 where id=p_id and state='pending' and expires_at>clock_timestamp() and processing_attempts<2
   and (processing_until is null or processing_until<clock_timestamp())
 returning * into a;
 if not found then return null; end if;
 return jsonb_build_object('id',a.id,'bucket','temple-photo-quarantine','path',a.original_path,
   'lease_token',v_token,'output_path','processed/'||a.id::text||'/'||v_token::text||'.jpg');
end $$;
-- Remove the former service completion route, which has no fencing token.
revoke all on function public.finish_temple_photo_processing(uuid,text) from service_role;
create function public.finish_leased_temple_photo(p_id uuid,p_token uuid) returns void
language plpgsql security definer set search_path='' as $$
declare v_path text:='processed/'||p_id::text||'/'||p_token::text||'.jpg';
begin
 if not exists(select 1 from storage.objects where bucket_id='temple-photo-quarantine' and name=v_path) then
   raise exception 'normalized_object_missing' using errcode='22023'; end if;
 update temple_private.photo_assets set state='processed',normalized_path=v_path,
   processing_token=null,processing_until=null
 where id=p_id and state='pending' and processing_token=p_token and processing_until>clock_timestamp();
 if not found then raise exception 'processing_lease_lost' using errcode='40001'; end if;
end $$;
revoke all on function public.claim_private_temple_photo(uuid),public.finish_leased_temple_photo(uuid,uuid) from public,anon,authenticated;
grant execute on function public.claim_private_temple_photo(uuid),public.finish_leased_temple_photo(uuid,uuid) to service_role;
