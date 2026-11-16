-- Consistent admin-management snapshot. Private read only, no activation.
create function public.get_temple_photo_management() returns jsonb
language plpgsql stable security definer set search_path='' as $$
begin
 if not public.is_admin() then raise exception 'admin_required' using errcode='42501'; end if;
 return jsonb_build_object(
  'gallery_revision',(select revision from public.content_meta where section='gallery'),
  'requests_enabled',(select enabled from temple_private.photo_publication_config where id=true),
  'delivery_enabled',(select delivery_enabled from temple_private.photo_publication_config where id=true),
  'photos',(select coalesce(jsonb_agg(jsonb_build_object(
   'id',a.id,'state',a.state,'created_at',a.created_at,'expires_at',a.expires_at,
   'preview_path',case when a.state in ('processed','reviewed') then a.normalized_path else null end,
   'publication_id',r.id,'publication_state',case when m.retired_at is not null then 'retired' else r.state end,
   'caption_ml',coalesce(m.caption_ml,r.caption_ml,''),'caption_en',coalesce(m.caption_en,r.caption_en,'')
   ) order by a.created_at desc),'[]'::jsonb)
   from temple_private.photo_assets a
   left join temple_private.photo_publication_requests r on r.asset_id=a.id
   left join temple_private.photo_gallery_manifest m on m.publication_id=r.id)
 );
end $$;
revoke all on function public.get_temple_photo_management() from public,anon,authenticated,service_role;
grant execute on function public.get_temple_photo_management() to authenticated;
