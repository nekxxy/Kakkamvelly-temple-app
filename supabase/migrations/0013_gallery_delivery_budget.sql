-- Payload/request reservations for the disabled image gateway. This is NOT a
-- claim to cap a provider invoice: routing, invocations, metadata/HTTP overhead,
-- other project traffic and attacks rejected before this RPC need edge controls.
create table temple_private.gallery_delivery_budget (
 id boolean primary key default true check(id),enabled boolean not null default false,
 hourly_requests integer not null default 0 check(hourly_requests between 0 and 10000),
 daily_payload_bytes bigint not null default 0 check(daily_payload_bytes between 0 and 1073741824),
 monthly_payload_bytes bigint not null default 0 check(monthly_payload_bytes between 0 and 10737418240),
 valid_until timestamptz
);
insert into temple_private.gallery_delivery_budget(id) values(true);
create table temple_private.gallery_delivery_reservations (
 hour_utc timestamptz primary key, requests integer not null check(requests>=0),
 payload_bytes bigint not null check(payload_bytes>=0)
);
alter table temple_private.gallery_delivery_budget enable row level security;
alter table temple_private.gallery_delivery_reservations enable row level security;
revoke all on temple_private.gallery_delivery_budget,temple_private.gallery_delivery_reservations from public,anon,authenticated,service_role;
create function public.reserve_temple_gallery_delivery() returns boolean
language plpgsql security definer set search_path='' as $$
declare c temple_private.gallery_delivery_budget%rowtype;
 t timestamptz:=clock_timestamp(); h timestamptz; d timestamptz; m timestamptz;
 count_h bigint; bytes_d bigint; bytes_m bigint; reserve_bytes constant bigint:=4194304;
begin
 select * into c from temple_private.gallery_delivery_budget where id=true for update;
 if not found or not c.enabled or c.valid_until is null or c.valid_until<=t or c.hourly_requests<1
  or c.daily_payload_bytes<reserve_bytes or c.monthly_payload_bytes<reserve_bytes then return false; end if;
 if not exists(select 1 from temple_private.photo_publication_config where id=true and delivery_enabled) then return false; end if;
 h:=date_trunc('hour',t at time zone 'UTC') at time zone 'UTC';
 d:=date_trunc('day',t at time zone 'UTC') at time zone 'UTC';
 m:=date_trunc('month',t at time zone 'UTC') at time zone 'UTC';
 select coalesce(sum(requests),0) into count_h from temple_private.gallery_delivery_reservations where hour_utc=h;
 select coalesce(sum(payload_bytes),0) into bytes_d from temple_private.gallery_delivery_reservations where hour_utc>=d and hour_utc<d+interval '1 day';
 select coalesce(sum(payload_bytes),0) into bytes_m from temple_private.gallery_delivery_reservations where hour_utc>=m and hour_utc<(m at time zone 'UTC'+interval '1 month') at time zone 'UTC';
 if count_h>=c.hourly_requests or bytes_d+reserve_bytes>c.daily_payload_bytes or bytes_m+reserve_bytes>c.monthly_payload_bytes then return false; end if;
 insert into temple_private.gallery_delivery_reservations(hour_utc,requests,payload_bytes) values(h,1,reserve_bytes)
 on conflict(hour_utc) do update set requests=temple_private.gallery_delivery_reservations.requests+1,payload_bytes=temple_private.gallery_delivery_reservations.payload_bytes+reserve_bytes;
 -- Reserve the full maximum, even for HEAD/not-found/failed reads. Never refund
 -- uncertain outcomes or claim provider billing reconciliation from this ledger.
 return true;
end $$;
revoke all on function public.reserve_temple_gallery_delivery() from public,anon,authenticated,service_role;
grant execute on function public.reserve_temple_gallery_delivery() to service_role;
