-- Transactional admin publish (production-safety rework of delete-all-then-insert).
--
-- One RPC per content section. Each call:
--   * verifies the ACTUAL CALLER is an admin (is_admin() on auth.uid() inside the
--     definer function; auth.uid() reflects the caller's JWT, not the function owner);
--   * locks the section's content_meta row (SELECT ... FOR UPDATE) so concurrent
--     publishers serialize instead of interleaving deletes/inserts;
--   * rejects stale overwrites via an expected-revision check (revision conflict);
--   * replaces rows in ONE transaction: any failure rolls the whole publish back,
--     so existing data is preserved on error and concurrent readers never see a
--     partially replaced section;
--   * bumps revision and resets verified=false in the same transaction (new
--     content is unverified until an admin re-verifies after committee review).
--
-- Hardening: security definer with empty search_path and fully schema-qualified
-- references (pg_catalog is still implicitly searched first for built-ins);
-- execute granted to authenticated only, revoked from public and anon. The
-- function owner bypasses RLS, so the is_admin() check inside is the sole gate.
--
-- Identity: row ids are regenerated per publish (generated-always identity);
-- sort_order defines display order. This is intentional: content identity is the
-- section payload, not surrogate keys. Nothing here inserts real content.

alter table public.content_meta add column if not exists revision bigint not null default 0;

create or replace function public.publish_poojas(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'poojas'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:poojas' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section poojas is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.poojas;
  insert into public.poojas (session, name_ml, name_en, time_label, hour, minute, rate_inr, active, sort_order)
  select r.session, r.name_ml, r.name_en, r.time_label, r.hour, r.minute, r.rate_inr, coalesce(r.active, true), coalesce(r.sort_order, 0)
    from jsonb_populate_recordset(null::public.poojas, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'poojas'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_poojas(jsonb, bigint) from public, anon;
grant execute on function public.publish_poojas(jsonb, bigint) to authenticated;

create or replace function public.publish_contacts(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'contacts'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:contacts' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section contacts is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.contacts;
  insert into public.contacts (role_ml, role_en, phone, whatsapp, sort_order)
  select r.role_ml, r.role_en, r.phone, r.whatsapp, coalesce(r.sort_order, 0)
    from jsonb_populate_recordset(null::public.contacts, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'contacts'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_contacts(jsonb, bigint) from public, anon;
grant execute on function public.publish_contacts(jsonb, bigint) to authenticated;

create or replace function public.publish_events(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'events'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:events' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section events is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.events;
  insert into public.events (icon, title_ml, title_en, time_ml, time_en, event_date, active)
  select r.icon, r.title_ml, r.title_en, r.time_ml, r.time_en, r.event_date, coalesce(r.active, true)
    from jsonb_populate_recordset(null::public.events, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'events'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_events(jsonb, bigint) from public, anon;
grant execute on function public.publish_events(jsonb, bigint) to authenticated;

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

create or replace function public.publish_announcements(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'announcements'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:announcements' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section announcements is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.announcements;
  insert into public.announcements (title_ml, title_en, body_ml, body_en, active)
  select r.title_ml, r.title_en, r.body_ml, r.body_en, coalesce(r.active, true)
    from jsonb_populate_recordset(null::public.announcements, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'announcements'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_announcements(jsonb, bigint) from public, anon;
grant execute on function public.publish_announcements(jsonb, bigint) to authenticated;

create or replace function public.publish_vazhipad(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'vazhipad'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:vazhipad' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section vazhipad is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.vazhipad;
  insert into public.vazhipad (name_ml, name_en, amount_inr, description_ml, active, sort_order)
  select r.name_ml, r.name_en, r.amount_inr, r.description_ml, coalesce(r.active, true), coalesce(r.sort_order, 0)
    from jsonb_populate_recordset(null::public.vazhipad, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'vazhipad'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_vazhipad(jsonb, bigint) from public, anon;
grant execute on function public.publish_vazhipad(jsonb, bigint) to authenticated;

create or replace function public.publish_festivals(p_rows jsonb, p_expected_revision bigint)
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
    where cm.section = 'festivals'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:festivals' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section festivals is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_rows) is distinct from 'array' then
    raise exception 'rows_must_be_array' using errcode = '22023';
  end if;

  delete from public.festivals;
  insert into public.festivals (name_ml, name_en, icon, date_utc_ms, note_ml, is_special, active, sort_order)
  select r.name_ml, r.name_en, r.icon, r.date_utc_ms, r.note_ml, coalesce(r.is_special, false), coalesce(r.active, true), coalesce(r.sort_order, 0)
    from jsonb_populate_recordset(null::public.festivals, p_rows) r;

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'festivals'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_festivals(jsonb, bigint) from public, anon;
grant execute on function public.publish_festivals(jsonb, bigint) to authenticated;

create or replace function public.publish_darshan_settings(p_row jsonb, p_expected_revision bigint)
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_revision bigint;
  r public.darshan_settings%rowtype;
begin
  if not public.is_admin() then
    raise exception 'admin_required' using errcode = '42501';
  end if;

  select cm.revision into v_revision
    from public.content_meta cm
    where cm.section = 'darshan_settings'
    for update of cm;

  if v_revision is null then
    raise exception 'unknown_section:darshan_settings' using errcode = '22023';
  end if;

  if v_revision is distinct from p_expected_revision then
    raise exception 'revision_conflict: section darshan_settings is at revision %, caller expected %',
      v_revision, p_expected_revision using errcode = '40001';
  end if;

  if jsonb_typeof(p_row) is distinct from 'object' then
    raise exception 'row_must_be_object' using errcode = '22023';
  end if;

  r := jsonb_populate_record(null::public.darshan_settings, p_row);

  delete from public.darshan_settings;
  insert into public.darshan_settings (id, morning_open_min, morning_close_min, evening_open_min, evening_close_min)
  values (1, r.morning_open_min, r.morning_close_min, r.evening_open_min, r.evening_close_min);

  update public.content_meta cm
     set verified = false,
         verified_at = null,
         verified_by = null,
         revision = cm.revision + 1
   where cm.section = 'darshan_settings'
   returning cm.revision into v_revision;

  return v_revision;
end;
$$;

revoke all on function public.publish_darshan_settings(jsonb, bigint) from public, anon;
grant execute on function public.publish_darshan_settings(jsonb, bigint) to authenticated;
