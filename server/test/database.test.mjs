import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { PGlite } from '@electric-sql/pglite';
import pg from 'pg';
const real = !!process.env.TEST_DATABASE_URL;
const db = real ? new pg.Pool({connectionString:process.env.TEST_DATABASE_URL,max:20}) : new PGlite();
const q = async (s,p=[]) => db.query(s,p);
const exec = async s => real ? db.query(s) : db.exec(s);
after(async()=>real?db.end():db.close());
await exec(`
create role anon; create role authenticated; create role service_role;
create schema auth; create table auth.users(id uuid primary key);
create function auth.uid() returns uuid language sql as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
grant usage on schema auth to authenticated,anon,service_role;
grant execute on function auth.uid() to authenticated,anon,service_role;
create table public.admin_users(user_id uuid primary key);
create function public.is_admin() returns boolean language sql security definer set search_path='' as $$ select exists(select 1 from public.admin_users where user_id=auth.uid()) $$;
create schema storage;
create table storage.buckets(id text primary key,name text,public boolean,file_size_limit bigint,allowed_mime_types text[]);
create table storage.objects(id uuid default gen_random_uuid(),bucket_id text,name text,unique(bucket_id,name));
alter table storage.objects enable row level security;
grant usage on schema storage to authenticated,anon,service_role;
grant select,insert,update,delete on storage.objects to authenticated,anon;
insert into auth.users values('00000000-0000-0000-0000-000000000001'),('00000000-0000-0000-0000-000000000002');
insert into public.admin_users values('00000000-0000-0000-0000-000000000001');
`);
for (const file of ['0006_visitor_dispatch_budget.sql','0007_private_photo_slots.sql']) await exec(await readFile(new URL('../../supabase/migrations/'+file,import.meta.url),'utf8'));
const recipient='a'.repeat(64); let seq=0;
const key=()=> (++seq).toString(16).padStart(64,'0');
const reserve=async (k=key(),r=recipient)=>(await q('select public.reserve_visitor_otp($1,$2) as value',[k,r])).rows[0].value;
async function reset(){await exec(`truncate temple_private.otp_reservations; update temple_private.otp_config set enabled=true,reserve_minor=2,daily_minor=10,monthly_minor=20,hourly_recipient=10,daily_recipient=10,rate_valid_until=clock_timestamp()+interval '1 day',allowed_recipient_hmacs=array['${recipient}'];`);}
// Each role probe uses its own connection/transaction so it works with real PG.
async function asUser(role,uid,sql,params=[]){
 const c=real?await db.connect():db;
 try {await c.query('begin');await c.query(`set local role ${role}`);await c.query("select set_config('request.jwt.claim.sub',$1,true)",[uid]);const r=await c.query(sql,params);await c.query('rollback');return r;}
 catch(e){await c.query('rollback');throw e;}finally{if(real)c.release();}
}
test('migration defaults refuse dispatch and uploads',async()=>{
 assert.equal((await reserve()).status,'disabled');
 await assert.rejects(asUser('authenticated','00000000-0000-0000-0000-000000000001','select public.create_photo_upload_slot()'),/photo_upload_disabled/);
});
test('client roles cannot reserve, complete or inspect ledger',async()=>{
 for(const role of ['anon','authenticated']){
  await assert.rejects(asUser(role,'','select public.reserve_visitor_otp($1,$2)',[key(),recipient]),/permission denied/);
  await assert.rejects(asUser(role,'','select * from temple_private.otp_reservations'),/permission denied/);
  await assert.rejects(asUser(role,'',"select public.record_visitor_otp(gen_random_uuid(),'accepted')"),/permission denied/);
 }
});
test('unknown destination and stale rates fail closed',async()=>{
 await reset();assert.equal((await reserve(key(),'b'.repeat(64))).status,'destination_not_approved');
 await exec("update temple_private.otp_config set rate_valid_until=clock_timestamp()-interval '1 second'");
 assert.equal((await reserve()).status,'configuration_required');
});
test('concurrent reservations cannot exceed monetary cap',async()=>{
 await reset();const results=await Promise.all(Array.from({length:20},()=>reserve()));
 assert.equal(results.filter(x=>x.status==='reserved').length,5);
 assert.equal(Number((await q('select sum(reserved_minor) as n from temple_private.otp_reservations')).rows[0].n),10);
});
test('concurrent duplicate delivery reserves once',async()=>{
 await reset();const k=key();const results=await Promise.all(Array.from({length:20},()=>reserve(k)));
 assert.equal(results.filter(x=>x.status==='reserved').length,1);
 assert.equal(results.filter(x=>x.status==='duplicate').length,19);
});
test('rolling per-recipient count limits sends independently of money',async()=>{
 await reset();await exec('update temple_private.otp_config set hourly_recipient=1');
 assert.equal((await reserve()).status,'reserved');assert.equal((await reserve()).status,'limited');
});
test('unknown outcome retains full reservation and cannot be rewritten',async()=>{
 await reset();const r=await reserve();await q("select public.record_visitor_otp($1,'unknown')",[r.id]);
 assert.equal(Number((await q('select sum(reserved_minor) as n from temple_private.otp_reservations')).rows[0].n),2);
 await assert.rejects(q("select public.record_visitor_otp($1,'accepted')",[r.id]),/reservation_not_pending/);
});
test('private bucket cannot be read by visitor or anonymous user',async()=>{
 const b=(await q("select * from storage.buckets where id='temple-photo-quarantine'")).rows[0];assert.equal(b.public,false);
 await exec("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine','secret-original')");
 for(const role of ['anon','authenticated']) assert.equal((await asUser(role,'00000000-0000-0000-0000-000000000002','select * from storage.objects')).rows.length,0);
});
test('visitor cannot allocate photo slots even when enabled',async()=>{
 await exec('update temple_private.photo_config set enabled=true');
 await assert.rejects(asUser('authenticated','00000000-0000-0000-0000-000000000002','select public.create_photo_upload_slot()'),/admin_required/);
});
test('admin slot is path-bound and does not expose read/overwrite',async()=>{
 // Commit a slot via superuser with the same JWT claim for later RLS tests.
 await q("select set_config('request.jwt.claim.sub',$1,false)",['00000000-0000-0000-0000-000000000001']);
 // pool connections may differ, so claim and function must share transaction.
 const c=real?await db.connect():db;let slot;
 try{await c.query('begin');await c.query("select set_config('request.jwt.claim.sub',$1,true)",['00000000-0000-0000-0000-000000000001']);slot=(await c.query('select public.create_photo_upload_slot() as s')).rows[0].s;await c.query('commit');}finally{if(real)c.release();}
 const uid='00000000-0000-0000-0000-000000000001';
 assert.equal((await asUser('authenticated',uid,'select public.can_upload_temple_photo($1,$2) as ok',[slot.bucket,slot.path])).rows[0].ok,true);
 await assert.rejects(asUser('authenticated',uid,'insert into storage.objects(bucket_id,name) values($1,$2)',[slot.bucket,'other-path']),/row-level security/);
 await asUser('authenticated',uid,'insert into storage.objects(bucket_id,name) values($1,$2)',[slot.bucket,slot.path]);
 assert.equal((await asUser('authenticated',uid,'select * from storage.objects')).rows.length,0);
 await assert.rejects(asUser('authenticated',uid,'select public.finish_temple_photo_processing($1,$2)',[slot.id,'processed/'+slot.id+'.jpg']),/permission denied/);
 await assert.rejects(asUser('authenticated',uid,'select public.review_temple_photo($1)',[slot.id]),/photo_not_processed/);
 await q("update temple_private.photo_assets set expires_at=clock_timestamp()-interval '1 second' where id=$1",[slot.id]);
 assert.equal((await asUser('authenticated',uid,'select public.can_upload_temple_photo($1,$2) as ok',[slot.bucket,slot.path])).rows[0].ok,false);
});
