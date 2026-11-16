-- Prepared only. Never enables a sender or authorizes a budget. Apply only after
-- review. HTTP hook RPCs commit independently of the enclosing Auth transaction.
create schema if not exists temple_private;
revoke all on schema temple_private from public, anon, authenticated;
create table temple_private.otp_config (
  id boolean primary key default true check(id),
  enabled boolean not null default false,
  currency text not null default 'INR' check(currency ~ '^[A-Z]{3}$'),
  reserve_minor bigint not null default 0 check(reserve_minor >= 0),
  daily_minor bigint not null default 0 check(daily_minor >= 0),
  monthly_minor bigint not null default 0 check(monthly_minor >= 0),
  hourly_recipient integer not null default 0 check(hourly_recipient >= 0),
  daily_recipient integer not null default 0 check(daily_recipient >= 0),
  rate_valid_until timestamptz,
  allowed_recipient_hmacs text[] not null default '{}'
);
insert into temple_private.otp_config(id) values(true);
create table temple_private.otp_reservations (
  id uuid primary key default gen_random_uuid(),
  delivery_hmac text unique not null check(delivery_hmac ~ '^[a-f0-9]{64}$'),
  recipient_hmac text not null check(recipient_hmac ~ '^[a-f0-9]{64}$'),
  reserved_minor bigint not null check(reserved_minor > 0),
  currency text not null,
  created_at timestamptz not null default clock_timestamp(),
  outcome text not null default 'reserved' check(outcome in ('reserved','accepted','unknown'))
);
create index otp_reservations_recipient_time on temple_private.otp_reservations(recipient_hmac,created_at);
create index otp_reservations_time on temple_private.otp_reservations(created_at);
alter table temple_private.otp_config enable row level security;
alter table temple_private.otp_reservations enable row level security;
revoke all on all tables in schema temple_private from public, anon, authenticated, service_role;

create function public.reserve_visitor_otp(p_delivery_hmac text, p_recipient_hmac text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  c temple_private.otp_config%rowtype;
  t timestamptz;
  v_id uuid;
  day_start timestamptz;
  month_start timestamptz;
begin
  -- Serialize all reservations with the config row. Never a process-local lock.
  select * into c from temple_private.otp_config where id = true for update;
  if not found or not c.enabled then return '{"status":"disabled"}'::jsonb; end if;
  t:=clock_timestamp();
  day_start:=date_trunc('day',t at time zone 'UTC') at time zone 'UTC';
  month_start:=date_trunc('month',t at time zone 'UTC') at time zone 'UTC';
  if p_delivery_hmac is null or p_delivery_hmac !~ '^[a-f0-9]{64}$'
     or p_recipient_hmac is null or p_recipient_hmac !~ '^[a-f0-9]{64}$' then
    return '{"status":"invalid"}'::jsonb;
  end if;
  if c.reserve_minor <= 0 or c.daily_minor <= 0 or c.monthly_minor <= 0
     or c.hourly_recipient <= 0 or c.daily_recipient <= 0
     or c.rate_valid_until is null or c.rate_valid_until <= t then
    return '{"status":"configuration_required"}'::jsonb;
  end if;
  if not (p_recipient_hmac = any(c.allowed_recipient_hmacs)) then
    return '{"status":"destination_not_approved"}'::jsonb;
  end if;
  if exists(select 1 from temple_private.otp_reservations where delivery_hmac=p_delivery_hmac) then
    return (select jsonb_build_object('status','duplicate','outcome',outcome)
      from temple_private.otp_reservations where delivery_hmac=p_delivery_hmac);
  end if;
  -- Numeric sums avoid bigint overflow. All outcomes retain their full cost.
  if (select coalesce(sum(reserved_minor),0) from temple_private.otp_reservations where created_at>=day_start)
       + c.reserve_minor > c.daily_minor
     or (select coalesce(sum(reserved_minor),0) from temple_private.otp_reservations where created_at>=month_start)
       + c.reserve_minor > c.monthly_minor
     or (select count(*) from temple_private.otp_reservations where recipient_hmac=p_recipient_hmac and created_at>t-interval '1 hour') >= c.hourly_recipient
     or (select count(*) from temple_private.otp_reservations where recipient_hmac=p_recipient_hmac and created_at>t-interval '24 hours') >= c.daily_recipient then
    return '{"status":"limited"}'::jsonb;
  end if;
  insert into temple_private.otp_reservations(delivery_hmac,recipient_hmac,reserved_minor,currency)
    values(p_delivery_hmac,p_recipient_hmac,c.reserve_minor,c.currency) returning id into v_id;
  return jsonb_build_object('status','reserved','id',v_id);
end $$;

create function public.record_visitor_otp(p_id uuid, p_outcome text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if p_outcome not in ('accepted','unknown') or p_outcome is null then
    raise exception 'invalid_outcome' using errcode='22023';
  end if;
  -- A repeated completion cannot rewrite an earlier outcome or refund cost.
  update temple_private.otp_reservations set outcome=p_outcome where id=p_id and outcome='reserved';
  if not found then raise exception 'reservation_not_pending' using errcode='22023'; end if;
end $$;
revoke all on function public.reserve_visitor_otp(text,text) from public, anon, authenticated;
revoke all on function public.record_visitor_otp(uuid,text) from public, anon, authenticated;
grant execute on function public.reserve_visitor_otp(text,text) to service_role;
grant execute on function public.record_visitor_otp(uuid,text) to service_role;
