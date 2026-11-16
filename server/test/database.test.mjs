import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { PGlite } from '@electric-sql/pglite';
import pg from 'pg';
import { randomUUID } from 'node:crypto';
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
create table public.content_meta(section text primary key,revision bigint not null default 0,verified boolean not null default true,verified_at timestamptz,verified_by uuid);
insert into public.content_meta(section) values('gallery');
create schema storage;
create table storage.buckets(id text primary key,name text,public boolean,file_size_limit bigint,allowed_mime_types text[]);
create table storage.objects(id uuid default gen_random_uuid(),bucket_id text,name text,unique(bucket_id,name));
alter table storage.objects enable row level security;
grant usage on schema storage to authenticated,anon,service_role;
grant select,insert,update,delete on storage.objects to authenticated,anon;
insert into auth.users values('00000000-0000-0000-0000-000000000001'),('00000000-0000-0000-0000-000000000002');
insert into public.admin_users values('00000000-0000-0000-0000-000000000001');
`);
for (const file of ['0006_visitor_dispatch_budget.sql','0007_private_photo_slots.sql','0008_private_photo_cleanup.sql','0009_private_photo_worker_lease.sql','0010_photo_publication_intent.sql','0011_photo_publication_manifest.sql','0012_photo_retirement_captions.sql','0013_gallery_delivery_budget.sql','0014_photo_management_snapshot.sql','0015_expired_upload_tombstones.sql']) await exec(await readFile(new URL('../../supabase/migrations/'+file,import.meta.url),'utf8'));
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

test('only processed derivative has admin-only preview, never original',async()=>{
 const uid='00000000-0000-0000-0000-000000000001';
 const id='00000000-0000-0000-0000-000000000088';
 const path=`processed/${id}.jpg`;
 await q("insert into temple_private.photo_assets(id,created_by,original_path,state,normalized_path) values($1,$2,$3,'processed',$4)",[id,uid,`${uid}/${id}/original`,path]);
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[path]);
 assert.equal((await asUser('authenticated',uid,'select public.can_preview_temple_photo($1,$2) as ok',['temple-photo-quarantine',path])).rows[0].ok,true);
 assert.equal((await asUser('authenticated','00000000-0000-0000-0000-000000000002','select public.can_preview_temple_photo($1,$2) as ok',['temple-photo-quarantine',path])).rows[0].ok,false);
 const rows=await asUser('authenticated',uid,'select name from storage.objects');assert.deepEqual(rows.rows.map(x=>x.name),[path]);
 const list=await asUser('authenticated',uid,'select public.list_private_temple_photos() as photos');assert.ok(list.rows[0].photos.some(x=>x.preview_path===path));
 await assert.rejects(asUser('authenticated','00000000-0000-0000-0000-000000000002','select public.list_private_temple_photos()'),/admin_required/);
});
test('all asset states count toward total capacity, not just pending',async()=>{
 await exec("truncate temple_private.photo_assets cascade; insert into temple_private.photo_assets(created_by,original_path,state,normalized_path) select '00000000-0000-0000-0000-000000000001'::uuid,'fixture-'||i,'processed','processed-'||i from generate_series(1,24) i");
 await assert.rejects(asUser('authenticated','00000000-0000-0000-0000-000000000001','select public.create_photo_upload_slot()'),/photo_slots_full/);
});

test('original cleanup requires processed asset and confirmed storage absence',async()=>{
 const id='00000000-0000-0000-0000-000000000077';const uid='00000000-0000-0000-0000-000000000001';const path=`${uid}/${id}/original`;
 await q("insert into temple_private.photo_assets(id,created_by,original_path,state,normalized_path) values($1,$2,$3,'processed',$4)",[id,uid,path,`processed/${id}.jpg`]);
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[path]);
 await assert.rejects(q('select public.confirm_private_photo_original_erased($1)',[id]),/original_still_exists/);
 await assert.rejects(asUser('authenticated',uid,'select public.private_photo_cleanup_batch()'),/permission denied/);
 await q("delete from storage.objects where bucket_id='temple-photo-quarantine' and name=$1",[path]);
 await q('select public.confirm_private_photo_original_erased($1)',[id]);
 assert.ok((await q('select original_erased_at from temple_private.photo_assets where id=$1',[id])).rows[0].original_erased_at);
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_assets where id=$1',[id])).rows[0].n),1);
});

test('currency cannot silently change with active reservation history',async()=>{
 await reset();assert.equal((await reserve()).status,'reserved');
 await exec("update temple_private.otp_config set currency='USD'");
 assert.equal((await reserve()).status,'configuration_required');
});

test('processing leases fence concurrent workers and late assertions',async()=>{
 await exec("truncate temple_private.photo_assets cascade; update temple_private.photo_config set enabled=true");
 const id='00000000-0000-0000-0000-000000000066';const uid='00000000-0000-0000-0000-000000000001';
 await q("insert into temple_private.photo_assets(id,created_by,original_path) values($1,$2,$3)",[id,uid,`${uid}/${id}/original`]);
 const claims=await Promise.all(Array.from({length:10},()=>q('select public.claim_private_temple_photo($1) as c',[id])));
 const wins=claims.map(x=>x.rows[0].c).filter(Boolean);assert.equal(wins.length,1);const c=wins[0];
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[c.output_path]);
 await q("update temple_private.photo_assets set processing_until=clock_timestamp()-interval '1 second' where id=$1",[id]);
 await assert.rejects(q('select public.finish_leased_temple_photo($1,$2)',[id,c.lease_token]),/processing_lease_lost/);
 const next=(await q('select public.claim_private_temple_photo($1) as c',[id])).rows[0].c;assert.notEqual(next.lease_token,c.lease_token);
 await assert.rejects(q('select public.finish_leased_temple_photo($1,$2)',[id,c.lease_token]),/processing_lease_lost/);
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[next.output_path]);
 await q('select public.finish_leased_temple_photo($1,$2)',[id,next.lease_token]);
 assert.equal((await q('select normalized_path from temple_private.photo_assets where id=$1',[id])).rows[0].normalized_path,next.output_path);
 await assert.rejects(asUser('service_role','','select public.finish_temple_photo_processing($1,$2)',[id,`processed/${id}.jpg`]),/permission denied/);
});

async function committedAdmin(sql,params=[]){
 const c=real?await db.connect():db;
 try{await c.query('begin');await c.query('set local role authenticated');await c.query("select set_config('request.jwt.claim.sub',$1,true)",['00000000-0000-0000-0000-000000000001']);const r=await c.query(sql,params);await c.query('commit');return r;}
 catch(e){await c.query('rollback');throw e;}finally{if(real)c.release();}
}
const queueSQL='select public.queue_reviewed_temple_photo($1,$2,$3,$4,$5) as r';
const publicationAsset='00000000-0000-0000-0000-000000000055';
const publicationRequest='00000000-0000-0000-0000-000000000044';
const adminUid='00000000-0000-0000-0000-000000000001';
const visitorUid='00000000-0000-0000-0000-000000000002';
test('publication intent is default disabled and inaccessible to visitor roles',async()=>{
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,[publicationRequest,publicationAsset,0,'ml','en']),/photo_publication_disabled/);
 await assert.rejects(asUser('authenticated',visitorUid,queueSQL,[publicationRequest,publicationAsset,0,'ml','en']),/admin_required/);
 await assert.rejects(asUser('anon','',queueSQL,[publicationRequest,publicationAsset,0,'ml','en']),/permission denied/);
 for(const role of ['anon','authenticated','service_role'])await assert.rejects(asUser(role,adminUid,'select * from temple_private.photo_publication_requests'),/permission denied/);
});
test('publication requires current gallery revision and reviewed object presence',async()=>{
 await exec('update temple_private.photo_publication_config set enabled=true');
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,[publicationRequest,publicationAsset,1,'ml','en']),/gallery_revision_conflict/);
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,[publicationRequest,publicationAsset,0,'ml','en']),/reviewed_photo_required/);
 await q("insert into temple_private.photo_assets(id,created_by,original_path,state,normalized_path,reviewed_by,reviewed_at) values($1,$2,$3,'reviewed',$4,$2,clock_timestamp())",[publicationAsset,adminUid,`${adminUid}/${publicationAsset}/original`,`processed/${publicationAsset}.jpg`]);
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,[publicationRequest,publicationAsset,0,'ml','en']),/reviewed_photo_required/);
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[`processed/${publicationAsset}.jpg`]);
});
test('concurrent publication retries persist once and never change captions or revision',async()=>{
 const results=await Promise.all(Array.from({length:8},()=>committedAdmin(queueSQL,[publicationRequest,publicationAsset,0,'മലയാളം','English'])));
 assert.ok(results.every(x=>x.rows[0].r.id===publicationRequest&&x.rows[0].r.state==='queued'));
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_publication_requests')).rows[0].n),1);
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,[publicationRequest,publicationAsset,0,'changed','English']),/publication_idempotency_conflict/);
 assert.equal(Number((await q("select revision from public.content_meta where section='gallery'")).rows[0].revision),0);
 assert.equal((await q("select public from storage.buckets where id='temple-photo-quarantine'")).rows[0].public,false);
 const listed=await asUser('authenticated',adminUid,'select public.list_temple_photo_publication_requests() as r');assert.equal(listed.rows[0].r[0].caption_ml,'മലയാളം');
 await assert.rejects(asUser('authenticated',visitorUid,'select public.list_temple_photo_publication_requests()'),/admin_required/);
});
test('cancellation is idempotent, works while disabled, and retry never resurrects intent',async()=>{
 await exec('update temple_private.photo_publication_config set enabled=false');
 await assert.rejects(asUser('authenticated',visitorUid,'select public.cancel_temple_photo_publication($1)',[publicationRequest]),/admin_required/);
 for(let i=0;i<2;i++)assert.equal((await committedAdmin('select public.cancel_temple_photo_publication($1) as r',[publicationRequest])).rows[0].r.state,'canceled');
 await exec('update temple_private.photo_publication_config set enabled=true');
 assert.equal((await committedAdmin(queueSQL,[publicationRequest,publicationAsset,0,'മലയാളം','English'])).rows[0].r.state,'canceled');
 await assert.rejects(asUser('authenticated',adminUid,queueSQL,['00000000-0000-0000-0000-000000000033',publicationAsset,0,'a'.repeat(2001),'English']),/invalid_publication_request/);
});

async function queuedPhoto(){
 const asset=randomUUID(),request=randomUUID(),source=`processed/${asset}.jpg`;
 const revision=Number((await q("select revision from public.content_meta where section='gallery'")).rows[0].revision);
 await q("insert into temple_private.photo_assets(id,created_by,original_path,state,normalized_path,reviewed_by,reviewed_at) values($1,$2,$3,'reviewed',$4,$2,clock_timestamp())",[asset,adminUid,`${adminUid}/${asset}/original`,source]);
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[source]);
 await committedAdmin(queueSQL,[request,asset,revision,'മലയാളം','An exact caption']);return {asset,request,revision};
}
const publicationClaim=async id=>(await q('select public.claim_temple_photo_publication($1) as r',[id])).rows[0].r;
const stageTarget=async claim=>q("insert into storage.objects(bucket_id,name) values('temple-photo-gallery',$1)",[claim.target_path]);
const publicPhotos=async()=>(await asUser('anon','','select public.get_uploaded_temple_gallery() as r')).rows[0].r;
test('delivery defaults disabled and gallery bucket remains private without direct reads',async()=>{
 assert.equal(await publicationClaim(publicationRequest),null);assert.deepEqual(await publicPhotos(),[]);
 assert.equal((await q("select public from storage.buckets where id='temple-photo-gallery'")).rows[0].public,false);
 for(const role of ['anon','authenticated']){
  await assert.rejects(asUser(role,adminUid,'select public.claim_temple_photo_publication($1)',[publicationRequest]),/permission denied/);
  await assert.rejects(asUser(role,adminUid,'select public.get_temple_gallery_photo_manifest($1,$2)',[publicationAsset,publicationRequest]),/permission denied/);
 }
});
test('fenced publication exposes manifest only with atomic revision and verification reset',async()=>{
 await exec('update temple_private.photo_publication_config set enabled=true,delivery_enabled=true');
 const p=await queuedPhoto();const claims=await Promise.all(Array.from({length:10},()=>publicationClaim(p.request)));
 const wins=claims.filter(Boolean);assert.equal(wins.length,1);const claim=wins[0];
 assert.equal(claim.source_bucket,'temple-photo-quarantine');assert.equal(claim.target_bucket,'temple-photo-gallery');
 await stageTarget(claim);
 assert.deepEqual(await publicPhotos(),[]);
 assert.equal((await q('select public.get_temple_gallery_photo_manifest($1,$2) as r',[p.asset,p.request])).rows[0].r,null);
 const rev=(await q('select public.finish_temple_photo_publication($1,$2) as r',[p.request,claim.lease_token])).rows[0].r;
 assert.equal(Number(rev),p.revision+1);assert.equal((await q("select verified from public.content_meta where section='gallery'")).rows[0].verified,false);
 const photos=await publicPhotos();assert.equal(photos.length,1);assert.equal(photos[0].caption_ml,'മലയാളം');assert.equal(photos[0].asset_id,p.asset);assert.equal(photos[0].path,undefined);
 const manifest=(await q('select public.get_temple_gallery_photo_manifest($1,$2) as r',[p.asset,p.request])).rows[0].r;assert.equal(manifest.path,claim.target_path);
 assert.equal((await asUser('anon','','select name from storage.objects where bucket_id=\'temple-photo-gallery\'')).rows.length,0);
 await assert.rejects(committedAdmin('select public.cancel_temple_photo_publication($1)',[p.request]),/photo_already_published/);
 await assert.rejects(q('select public.finish_temple_photo_publication($1,$2)',[p.request,claim.lease_token]),/publication_lease_lost/);
});
test('canceling claimed publication fences late completion without exposing staged object',async()=>{
 const p=await queuedPhoto(),claim=await publicationClaim(p.request);await stageTarget(claim);
 await committedAdmin('select public.cancel_temple_photo_publication($1)',[p.request]);
 await assert.rejects(q('select public.finish_temple_photo_publication($1,$2)',[p.request,claim.lease_token]),/publication_lease_lost/);
 assert.equal((await q('select public.get_temple_gallery_photo_manifest($1,$2) as r',[p.asset,p.request])).rows[0].r,null);
});
test('expired publication lease cannot finish and retry uses a different immutable target',async()=>{
 const p=await queuedPhoto(),first=await publicationClaim(p.request);await stageTarget(first);
 await q("update temple_private.photo_publication_requests set lease_until=clock_timestamp()-interval '1 second' where id=$1",[p.request]);
 const second=await publicationClaim(p.request);assert.notEqual(second.target_path,first.target_path);await stageTarget(second);
 await assert.rejects(q('select public.finish_temple_photo_publication($1,$2)',[p.request,first.lease_token]),/publication_lease_lost/);
 await q('select public.finish_temple_photo_publication($1,$2)',[p.request,second.lease_token]);
 assert.equal(await publicationClaim(p.request),null);
});
test('gallery revision changes while staging reject finalization without partial manifest',async()=>{
 const p=await queuedPhoto(),claim=await publicationClaim(p.request);await stageTarget(claim);
 await exec("update public.content_meta set revision=revision+1 where section='gallery'");
 await assert.rejects(q('select public.finish_temple_photo_publication($1,$2)',[p.request,claim.lease_token]),/gallery_revision_conflict/);
 assert.equal((await q('select public.get_temple_gallery_photo_manifest($1,$2) as r',[p.asset,p.request])).rows[0].r,null);
 assert.equal(await publicationClaim(p.request),null);
});
test('revoked admin membership prevents finalization even after successful staging',async()=>{
 const p=await queuedPhoto(),claim=await publicationClaim(p.request);await stageTarget(claim);
 await q('delete from public.admin_users where user_id=$1',[adminUid]);
 try{await assert.rejects(q('select public.finish_temple_photo_publication($1,$2)',[p.request,claim.lease_token]),/publication_authority_changed/);}
 finally{await q('insert into public.admin_users(user_id) values($1)',[adminUid]);}
});
test('delivery kill switch hides manifests and metadata, not only new requests',async()=>{
 assert.ok((await publicPhotos()).length>=1);await exec('update temple_private.photo_publication_config set delivery_enabled=false');
 assert.deepEqual(await publicPhotos(),[]);
 assert.equal((await q('select count(*) as n from temple_private.photo_gallery_manifest')).rows[0].n.toString(),'2');
});

async function publishedPhoto(){await exec('update temple_private.photo_publication_config set enabled=true,delivery_enabled=true');const p=await queuedPhoto(),claim=await publicationClaim(p.request);await stageTarget(claim);await q('select public.finish_temple_photo_publication($1,$2)',[p.request,claim.lease_token]);return p;}
const galleryRev=async()=>Number((await q("select revision from public.content_meta where section='gallery'")).rows[0].revision);
test('caption edit requires admin/current revision, preserves source approval and clears verification',async()=>{
 const p=await publishedPhoto(),rev=await galleryRev();
 const sql='select public.edit_temple_photo_captions($1,$2,$3,$4) as r';
 await assert.rejects(asUser('authenticated',visitorUid,sql,[p.request,rev,'ml','en']),/admin_required/);
 await assert.rejects(asUser('anon','',sql,[p.request,rev,'ml','en']),/permission denied/);
 await assert.rejects(committedAdmin(sql,[p.request,rev-1,'ml','en']),/gallery_revision_conflict/);
 await exec("update public.content_meta set verified=true where section='gallery'");
 assert.equal(Number((await committedAdmin(sql,[p.request,rev,'പുതിയ കുറിപ്പ്','New caption'])).rows[0].r),rev+1);
 assert.equal((await q("select verified from public.content_meta where section='gallery'")).rows[0].verified,false);
 const listed=(await publicPhotos()).find(x=>x.publication_id===p.request);assert.equal(listed.caption_en,'New caption');
 // Original approved request stays unchanged, and the revision-attributed edit is retained.
 assert.equal((await q('select caption_en from temple_private.photo_publication_requests where id=$1',[p.request])).rows[0].caption_en,'An exact caption');
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_gallery_changes where publication_id=$1',[p.request])).rows[0].n),1);
 assert.equal(Number((await committedAdmin(sql,[p.request,rev+1,'പുതിയ കുറിപ്പ്','New caption'])).rows[0].r),rev+1);
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_gallery_changes where publication_id=$1',[p.request])).rows[0].n),1);
});
test('concurrent caption updates use compare-and-swap, not last-writer-wins',async()=>{
 const p=await publishedPhoto(),rev=await galleryRev(),sql='select public.edit_temple_photo_captions($1,$2,$3,$4)';
 const results=await Promise.allSettled(['A','B'].map(x=>committedAdmin(sql,[p.request,rev,'',x])));
 assert.equal(results.filter(x=>x.status==='fulfilled').length,1);assert.equal(results.filter(x=>x.status==='rejected'&&/gallery_revision_conflict/.test(x.reason.message)).length,1);
});
test('retirement works with both feature switches off and prevents future gateway/metadata reads',async()=>{
 const p=await publishedPhoto(),rev=await galleryRev(),sql='select public.retire_temple_gallery_photo($1,$2) as r';
 await assert.rejects(asUser('authenticated',visitorUid,sql,[p.request,rev]),/admin_required/);
 await assert.rejects(committedAdmin(sql,[p.request,rev-1]),/gallery_revision_conflict/);
 await exec('update temple_private.photo_publication_config set enabled=false,delivery_enabled=false');
 assert.equal(Number((await committedAdmin(sql,[p.request,rev])).rows[0].r),rev+1);
 assert.equal(Number((await committedAdmin(sql,[p.request,rev])).rows[0].r),rev+1);
 await exec('update temple_private.photo_publication_config set enabled=true,delivery_enabled=true');
 assert.equal((await publicPhotos()).some(x=>x.publication_id===p.request),false);
 assert.equal((await q('select public.get_temple_gallery_photo_manifest($1,$2) as r',[p.asset,p.request])).rows[0].r,null);
 assert.ok((await q('select retired_at from temple_private.photo_gallery_manifest where publication_id=$1',[p.request])).rows[0].retired_at);
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_gallery_changes where publication_id=$1 and action=\'retire\'',[p.request])).rows[0].n),1);
 await assert.rejects(committedAdmin('select public.edit_temple_photo_captions($1,$2,$3,$4)',[p.request,rev+1,'','x']),/published_photo_missing/);
 // No object delete or quota reopening occurs on retirement.
 assert.equal(Number((await q("select count(*) as n from storage.objects where bucket_id='temple-photo-gallery' and name like $1",[`${p.asset}/%`])).rows[0].n),1);
});

test('gallery delivery reservation defaults disabled and never exposes a client budget mutation',async()=>{
 assert.equal((await q('select public.reserve_temple_gallery_delivery() as r')).rows[0].r,false);
 for(const role of ['anon','authenticated']){
  await assert.rejects(asUser(role,adminUid,'select public.reserve_temple_gallery_delivery()'),/permission denied/);
  await assert.rejects(asUser(role,adminUid,'select * from temple_private.gallery_delivery_reservations'),/permission denied/);
 }
});
test('concurrent image request reservations cannot overshoot payload or request ceilings',async()=>{
 await exec(`update temple_private.photo_publication_config set delivery_enabled=true;update temple_private.gallery_delivery_budget set enabled=true,hourly_requests=10,daily_payload_bytes=8388608,monthly_payload_bytes=16777216,valid_until=clock_timestamp()+interval '1 hour'`);
 const results=await Promise.all(Array.from({length:20},()=>q('select public.reserve_temple_gallery_delivery() as r')));
 assert.equal(results.filter(x=>x.rows[0].r).length,2);
 assert.equal(Number((await q('select sum(payload_bytes) as n from temple_private.gallery_delivery_reservations')).rows[0].n),8388608);
 // Growing byte allowance does not waive hourly request ceiling.
 await exec('update temple_private.gallery_delivery_budget set hourly_requests=2,daily_payload_bytes=16777216');
 assert.equal((await q('select public.reserve_temple_gallery_delivery() as r')).rows[0].r,false);
});
test('stale or zero budget and delivery kill switch fail closed without resetting ledger',async()=>{
 await exec("update temple_private.gallery_delivery_budget set hourly_requests=10,valid_until=clock_timestamp()-interval '1 second'");
 assert.equal((await q('select public.reserve_temple_gallery_delivery() as r')).rows[0].r,false);
 await exec("update temple_private.gallery_delivery_budget set valid_until=clock_timestamp()+interval '1 hour',daily_payload_bytes=0");
 assert.equal((await q('select public.reserve_temple_gallery_delivery() as r')).rows[0].r,false);
 await exec('update temple_private.gallery_delivery_budget set daily_payload_bytes=16777216;update temple_private.photo_publication_config set delivery_enabled=false');
 assert.equal((await q('select public.reserve_temple_gallery_delivery() as r')).rows[0].r,false);
 assert.equal(Number((await q('select sum(payload_bytes) as n from temple_private.gallery_delivery_reservations')).rows[0].n),8388608);
});

test('admin management snapshot distinguishes processing from publication/retirement and delivery',async()=>{
 for(const [role,uid] of [['anon',''],['authenticated',visitorUid],['service_role','']])await assert.rejects(asUser(role,uid,'select public.get_temple_photo_management()'),role==='authenticated'?/admin_required/:/permission denied/);
 const result=(await asUser('authenticated',adminUid,'select public.get_temple_photo_management() as r')).rows[0].r;
 assert.equal(Number(result.gallery_revision),await galleryRev());assert.equal(result.delivery_enabled,false);
 assert.equal(result.photos.length,Number((await q('select count(*) as n from temple_private.photo_assets')).rows[0].n));
 const retired=result.photos.find(x=>x.publication_state==='retired');assert.ok(retired);assert.equal(retired.state,'reviewed');assert.ok(retired.publication_id);assert.ok(retired.preview_path);
 const edited=result.photos.find(x=>x.caption_en==='New caption');assert.ok(edited);assert.equal(edited.publication_state,'published');
 assert.ok(result.photos.some(x=>x.publication_state==='canceled'));
 assert.ok(result.photos.every(x=>x.original_path===undefined&&x.requested_by===undefined&&x.reviewed_by===undefined));
});

test('expired pending reconciliation tombstones only old inactive slots, retaining capacity and live work',async()=>{
 const old=randomUUID(),fresh=randomUUID(),active=randomUUID();
 for(const id of [old,fresh,active])await q('insert into temple_private.photo_assets(id,created_by,original_path) values($1,$2,$3)',[id,adminUid,`${adminUid}/${id}/original`]);
 await q("update temple_private.photo_assets set expires_at=clock_timestamp()-interval '2 days' where id in ($1,$2)",[old,active]);
 await q("update temple_private.photo_assets set processing_until=clock_timestamp()+interval '1 minute',processing_token=gen_random_uuid() where id=$1",[active]);
 const before=Number((await q('select count(*) as n from temple_private.photo_assets')).rows[0].n);
 await assert.rejects(asUser('authenticated',adminUid,'select public.abandon_expired_temple_photo_uploads()'),/permission denied/);
 const rows=(await q('select public.abandon_expired_temple_photo_uploads() as r')).rows[0].r;
 assert.ok(rows.some(x=>x.id===old));assert.equal(rows.some(x=>x.id===fresh||x.id===active),false);
 assert.equal(Number((await q('select count(*) as n from temple_private.photo_assets')).rows[0].n),before);
 // Even extending a tombstone deadline cannot make the worker claim it again.
 await q("update temple_private.photo_assets set expires_at=clock_timestamp()+interval '1 day' where id=$1",[old]);
 assert.equal((await q('select public.claim_private_temple_photo($1) as r',[old])).rows[0].r,null);
 const path=`${adminUid}/${old}/original`;await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[path]);
 await assert.rejects(q('select public.confirm_abandoned_temple_photo_absent($1)',[old]),/original_still_exists/);
 await q("delete from storage.objects where bucket_id='temple-photo-quarantine' and name=$1",[path]);
 await q('select public.confirm_abandoned_temple_photo_absent($1)',[old]);
 const checked=(await q('select abandoned_absence_checked_at,original_erased_at from temple_private.photo_assets where id=$1',[old])).rows[0];assert.ok(checked.abandoned_absence_checked_at);assert.equal(checked.original_erased_at,null);
 assert.ok((await q('select public.abandon_expired_temple_photo_uploads() as r')).rows[0].r.some(x=>x.id===old));
 // A late Storage completion is detected next time, not hidden by an erasure flag.
 await q("insert into storage.objects(bucket_id,name) values('temple-photo-quarantine',$1)",[path]);
 await assert.rejects(q('select public.confirm_abandoned_temple_photo_absent($1)',[old]),/original_still_exists/);
 await assert.rejects(q('select public.confirm_abandoned_temple_photo_absent($1)',[fresh]),/photo_not_abandoned/);
});
