-- Vazhipad and festival content tables (structure only, no content rows).
-- The app's verification gate (REQUIRED_META_SECTIONS in SupabaseContentSource) already
-- requires content_meta markers for these sections; the tables complete the schema.
-- Public read via RLS; writes admin-only through is_admin(), same as the v1 tables.

create table if not exists public.vazhipad (
  id bigint generated always as identity primary key,
  name_ml text not null,
  name_en text not null,
  amount_inr numeric(10,2) not null,
  description_ml text,
  active boolean not null default true,
  sort_order int not null default 0
);

create table if not exists public.festivals (
  id bigint generated always as identity primary key,
  name_ml text not null,
  name_en text not null,
  icon text,
  date_utc_ms bigint not null,
  note_ml text,
  is_special boolean not null default false,
  active boolean not null default true,
  sort_order int not null default 0
);

alter table public.vazhipad enable row level security;
alter table public.festivals enable row level security;

create policy "public read vazhipad" on public.vazhipad for select using (true);
create policy "public read festivals" on public.festivals for select using (true);

create policy "admin write vazhipad" on public.vazhipad
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write festivals" on public.festivals
  for all using (public.is_admin()) with check (public.is_admin());

insert into public.content_meta (section) values
  ('vazhipad'), ('festivals')
on conflict (section) do nothing;
