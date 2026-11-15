-- 0005: server-side integrity for gallery photo references.
--
-- Why: the visitor app renders each gallery row as
--   https://kakkamvellytemple.page/images/<photo_path>.jpg
-- keeping only [a-z0-9-] characters from photo_path (GalleryScreen.imageUrl).
-- A value outside that charset would silently render the WRONG or a broken
-- image. Until now only the admin editor checked this client-side; a buggy or
-- hostile client could still write a bad row through publish_gallery (or any
-- future direct write route). The database now enforces the grounded rule
-- itself, independent of any editor.
--
-- GROUNDED constraint: photo_path charset/format (mirrors the visitor URL rule).
-- DESIGN ASSUMPTIONS (generous storage-abuse sanity bounds, NOT product
-- limits): 64-char photo_path ceiling, 2000-char caption ceilings, 200-row
-- batch cap. The editor's product defaults (24 photos, 80-char captions) stay
-- client-side and are stricter; if the product limits change, only the client
-- changes - these server bounds should not be tightened without a reason.

alter table public.gallery
  alter column photo_path set not null;

alter table public.gallery
  add constraint gallery_photo_path_format check (photo_path ~ '^[a-z0-9-]{1,64}$'),
  add constraint gallery_photo_path_unique unique (photo_path),
  add constraint gallery_caption_len check (
    length(coalesce(caption_ml, '')) <= 2000 and length(coalesce(caption_en, '')) <= 2000),
  add constraint gallery_sort_order_nonneg check (sort_order >= 0);

-- Pre-validate in the RPC so the editor gets a clear, specific error instead of
-- a bare constraint name, and so a bad batch is rejected BEFORE the delete runs
-- (the transaction would roll back either way; this keeps the failure legible).
create or replace function public.publish_gallery(p_rows jsonb, p_expected_revision bigint)
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_revision bigint;
begin
  if not public.is_admin() then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  select cm.revision into v_revision
    from public.content_meta cm
    where cm.section = 'gallery'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:gallery' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section gallery is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  if (select count(*) from jsonb_populate_recordset(null::public.gallery, p_rows) r) > 200 then
    raise exception 'too_many_photos: sanity cap 200 rows' using errcode = '22023';
  end if;

  if exists (select 1 from jsonb_populate_recordset(null::public.gallery, p_rows) r
             where r.photo_path is null or r.photo_path !~ '^[a-z0-9-]{1,64}$') then
    raise exception 'invalid_photo_path: only a-z, 0-9 and ''-'', 1-64 chars (visitor URL rule)' using errcode = '22023';
  end if;

  if exists (select r.photo_path from jsonb_populate_recordset(null::public.gallery, p_rows) r
             group by r.photo_path having count(*) > 1) then
    raise exception 'duplicate_photo_path' using errcode = '22023';
  end if;

  delete from public.gallery;
  insert into public.gallery (photo_path, caption_ml, caption_en, sort_order, active)
  select r.photo_path, r.caption_ml, r.caption_en, coalesce(r.sort_order, 0), coalesce(r.active, true)
    from jsonb_populate_recordset(null::public.gallery, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'gallery'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_gallery(jsonb, bigint) from public, anon;
grant execute on function public.publish_gallery(jsonb, bigint) to authenticated;
