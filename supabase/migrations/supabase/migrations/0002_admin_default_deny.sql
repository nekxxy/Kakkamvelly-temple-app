-- Admin roles, default-deny writes, and per-section verification markers.

-- Role membership. Managed only through the service role / dashboard; no client
-- policy allows inserting, updating or deleting membership rows.
create table if not exists public.admin_users (
  user_id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

alter table public.admin_users enable row level security;

-- A signed-in user may read only their own row, so the app can show or hide admin UI.
create policy "users read own admin row" on public.admin_users
  for select using (auth.uid() = user_id);

-- Role check used by the write policies. security definer so the policy does not
-- recurse into admin_users RLS; fixed search_path and schema-qualified references.
create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public
as $$ select exists (select 1 from public.admin_users where user_id = auth.uid()) $$;

-- Least-privilege execute: only the roles whose writes the policies evaluate.
revoke execute on function public.is_admin() from public;
grant execute on function public.is_admin() to anon, authenticated;

-- Per-section verification marker. The app shows its unverified-content notice until
-- every displayed section's marker is present and verified=true. Sections are marked
-- verified only by an authorized admin after the content has been checked.
create table if not exists public.content_meta (
  section text primary key,
  verified boolean not null default false,
  verified_at timestamptz,
  verified_by uuid references auth.users(id)
);

alter table public.content_meta enable row level security;
create policy "public read meta" on public.content_meta for select using (true);
create policy "admin write meta" on public.content_meta
  for all using (public.is_admin()) with check (public.is_admin());

insert into public.content_meta (section) values
  ('darshan_settings'), ('poojas'), ('contacts'), ('events'), ('gallery'), ('announcements')
on conflict (section) do nothing;

-- Only server-verified admins may write content. The v1 public read policies are
-- unchanged (policies are OR'd, so read stays public).
create policy "admin write darshan" on public.darshan_settings
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write poojas" on public.poojas
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write contacts" on public.contacts
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write events" on public.events
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write gallery" on public.gallery
  for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write announcements" on public.announcements
  for all using (public.is_admin()) with check (public.is_admin());
