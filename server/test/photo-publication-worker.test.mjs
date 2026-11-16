import { test } from 'node:test';import assert from 'node:assert/strict';
import { publishPhoto } from '../photo-publication-worker.mjs';
import { supabasePhotoPublicationAdapters } from '../photo-publication-adapters.mjs';
const id='00000000-0000-0000-0000-000000000001',asset='00000000-0000-0000-0000-000000000002',token='00000000-0000-0000-0000-000000000003';
const claim={id,asset_id:asset,lease_token:token,source_bucket:'temple-photo-quarantine',source_path:`processed/${asset}/${token}.jpg`,target_bucket:'temple-photo-gallery',target_path:`${asset}/${id}/${token}.jpg`};
function deps(){const calls=[],bytes=Buffer.from([255,216,255,217]);return{calls,bytes,enabled:true,id,publications:{claim:async()=>claim,finish:async(i,t)=>{calls.push(['finish',i,t]);return 1;}},storage:{downloadBounded:async(...a)=>{calls.push(['download',...a]);return bytes;},insert:async(b,p,buf,type)=>{calls.push(['insert',b,p,type]);assert.equal(buf[0],255);}}};}
test('publication worker defaults disabled without side effects',async()=>assert.equal((await publishPhoto()).status,'disabled'));
test('worker stages only reviewed derivative into private bucket then finalizes exact lease',async()=>{
 const d=deps();assert.deepEqual(await publishPhoto(d),{status:'published',revision:1});assert.deepEqual(d.calls.map(x=>x[0]),['download','insert','finish']);assert.equal(d.calls[2][2],token);assert.ok(d.bytes.every(x=>x===0));
});
test('worker refuses injected paths, wrong buckets and absent leases before any storage call',async()=>{
 for(const bad of [null,{...claim,lease_token:null},{...claim,source_path:'https://evil.invalid/a'},{...claim,target_bucket:'public'},{...claim,target_path:'../original'}]){
  const d=deps();d.publications.claim=async()=>bad;assert.equal((await publishPhoto(d)).status,'not_queued');assert.deepEqual(d.calls,[]);
 }
});
test('uncertain insert/finalization never retries or reports publication',async()=>{
 for(const stage of ['insert','finish']){const d=deps();let calls=0;d[stage==='insert'?'storage':'publications'][stage]=async()=>{calls++;throw Error('private detail');};assert.equal((await publishPhoto(d)).status,'publication_unconfirmed');assert.equal(calls,1);assert.ok(d.bytes.every(x=>x===0));}
 const d=deps();d.publications.finish=async()=>null;assert.equal((await publishPhoto(d)).status,'publication_unconfirmed');
});
test('adapters construct without network and reject arbitrary origins/paths',async()=>{
 let calls=0;const fetchImpl=async()=>{calls++;throw Error('not called');};
 assert.throws(()=>supabasePhotoPublicationAdapters({url:'https://evil.invalid',serviceKey:'test',fetchImpl}));
 const a=supabasePhotoPublicationAdapters({url:'https://test.supabase.co',serviceKey:'test',fetchImpl});assert.equal(calls,0);
 await assert.rejects(a.storage.insert('public',claim.target_path,Buffer.alloc(4),'image/jpeg'));
 await assert.rejects(a.storage.downloadBounded('temple-photo-gallery','../original',4194304));assert.equal(calls,0);
});
test('adapter uses immutable private insert and bounded current manifest RPC',async()=>{
 const calls=[];const fetchImpl=async(url,options)=>{calls.push({url,options});return new Response(JSON.stringify(null),{status:200,headers:{'Content-Type':'application/json'}});};
 const a=supabasePhotoPublicationAdapters({url:'https://test.supabase.co',serviceKey:'test',fetchImpl});
 await a.storage.insert('temple-photo-gallery',claim.target_path,Buffer.from([255,216,255,217]),'image/jpeg');
 assert.equal(calls[0].options.headers['x-upsert'],'false');assert.equal(calls[0].options.redirect,'error');assert.ok(calls[0].options.signal);assert.ok(calls[0].url.includes('/object/temple-photo-gallery/'));
 await a.manifest.lookup(asset,id);assert.ok(calls[1].url.endsWith('/rpc/get_temple_gallery_photo_manifest'));assert.deepEqual(JSON.parse(calls[1].options.body),{p_asset_id:asset,p_publication_id:id});
});
test('gallery adapter caps downloads even without a content-length header',async()=>{
 const a=supabasePhotoPublicationAdapters({url:'https://test.supabase.co',serviceKey:'test',fetchImpl:async()=>new Response(Buffer.alloc(5))});
 await assert.rejects(a.storage.downloadBounded('temple-photo-gallery',claim.target_path,4),/Derivative too large/);
});
