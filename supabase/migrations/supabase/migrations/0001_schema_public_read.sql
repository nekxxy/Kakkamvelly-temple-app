-- Kakkamvelly temple backend schema v1.
-- Structure only. Public read via RLS; writes reserved for authenticated admin roles
-- (added in 0002_admin_default_deny.sql).

create table if not exists public.darshan_settings (
  id smallint primary key default 1 check (id = 1),
  morning_open_min smallint not null,
  morning_close_min smallint not null,
  evening_open_min smallint not null,
  evening_close_min smallint not null,
  updated_at timestamptz not null default now()
);

create table if not exists public.poojas (
  id bigint generated always as identity primary key,
  session text not null check (session in ('morning','evening')),
  name_ml text not null,
  name_en text not null,
  time_label text,
  hour smallint,
  minute smallint,
  rate_inr numeric(10,2),
  active boolean not null default true,
  sort_order int not null default 0
);

create table if not exists public.contacts (
  id bigint generated always as identity primary key,
  role_ml text not null,
  role_en text not null,
  phone text,
  whatsapp text,
  sort_order int not null default 0
);

create table if not exists public.events (
  id bigint generated always as identity primary key,
  icon text,
  title_ml text not null,
  title_en text not null,
  time_ml text,
  time_en text,
  event_date date,
  active boolean not null default true
);

create table if not exists public.gallery (
  id bigint generated always as identity primary key,
  photo_path text,
  caption_ml text,
  caption_en text,
  sort_order int not null default 0,
  active boolean not null default true
);

create table if not exists public.announcements (
  id bigint generated always as identity primary key,
  title_ml text,
  title_en text not null,
  body_ml text,
  body_en text,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

alter table public.darshan_settings enable row level security;
alter table public.poojas enable row level security;
alter table public.contacts enable row level security;
alter table public.events enable row level security;
alter table public.gallery enable row level security;
alter table public.announcements enable row level security;

create policy "public read darshan" on public.darshan_settings for select using (true);
create policy "public read poojas" on public.poojas for select using (true);
create policy "public read contacts" on public.contacts for select using (true);
create policy "public read events" on public.events for select using (true);
create policy "public read gallery" on public.gallery for select using (true);
create policy "public read announcements" on public.announcements for select using (true);
