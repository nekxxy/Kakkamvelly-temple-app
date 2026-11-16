import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createGalleryPhotoHandler } from '../gallery-photo-handler.mjs';
const asset='00000000-0000-0000-0000-000000000001',publication='00000000-0000-0000-0000-000000000002',lease='00000000-0000-0000-0000-000000000003';
const path=`${asset}/${publication}/${lease}.jpg`;
const row={asset_id:asset,publication_id:publication,bucket:'temple-photo-gallery',path};
const url=`https://gallery.test/temple-gallery/${asset}/${publication}`;
const jpeg=Buffer.from([255,216,255,217]);
const req=(method='GET',u=url)=>new Request(u,{method});
test('gallery gateway defaults disabled without consulting private sources',async()=>{
 let calls=0;const h=createGalleryPhotoHandler({manifest:{lookup:()=>{calls++;throw Error('no');}}});
 assert.equal((await h(req())).status,404);assert.equal(calls,0);
});
test('gateway refuses arbitrary paths, query destinations and writes before lookup',async()=>{
 let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:()=>{calls++;}},storage:{downloadBounded(){}}});
 for(const u of [url+'?url=https://evil.invalid',url+'/extra','https://gallery.test/temple-gallery/../original',url.replace(publication,'..%2foriginal')])assert.equal((await h(req('GET',u))).status,404);
 assert.equal((await h(req('POST'))).status,405);assert.equal(calls,0);
});
test('private originals, wrong identifiers and arbitrary hosts never reach storage',async()=>{
 for(const bad of [null,{...row,bucket:'temple-photo-quarantine'},{...row,path:'https://evil.invalid/image'},{...row,path:`${asset}/${publication}/original`},{...row,asset_id:publication}]){
  let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>bad},storage:{downloadBounded:async()=>{calls++;return jpeg;}}});
  assert.equal((await h(req())).status,404);assert.equal(calls,0);
 }
});
test('only current manifest derivative returns uncached JPEG with fixed size bound',async()=>{
 let lookups=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async(a,p)=>{lookups++;assert.equal(a,asset);assert.equal(p,publication);return row;}},storage:{downloadBounded:async(b,p,max)=>{assert.equal(b,'temple-photo-gallery');assert.equal(p,path);assert.equal(max,4194304);return jpeg;}}});
 const response=await h(req());assert.equal(response.status,200);assert.equal(lookups,2);
 assert.equal(response.headers.get('content-type'),'image/jpeg');assert.equal(response.headers.get('cache-control'),'no-store');assert.equal(response.headers.get('x-content-type-options'),'nosniff');
 assert.deepEqual(Buffer.from(await response.arrayBuffer()),jpeg);
});
test('revocation while downloading does not release buffered bytes',async()=>{
 let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>++calls===1?row:null},storage:{downloadBounded:async()=>jpeg}});
 const response=await h(req());assert.equal(response.status,404);assert.equal(await response.text(),'');
});
test('changed immutable object while downloading fails closed',async()=>{
 let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>++calls===1?row:{...row,path:path.replace(lease,publication)}},storage:{downloadBounded:async()=>jpeg}});
 assert.equal((await h(req())).status,404);
});
test('HEAD consults manifest but never downloads and exposes no private object path',async()=>{
 let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>row},storage:{downloadBounded:async()=>{calls++;return jpeg;}}});
 const response=await h(req('HEAD'));assert.equal(response.status,200);assert.equal(calls,0);assert.equal(await response.text(),'');assert.equal(response.headers.get('location'),null);
});
test('oversize, malformed output and unavailable sources expose no internal errors',async()=>{
 for(const bytes of [Buffer.alloc(4194305),Buffer.from('not-jpeg'),null]){
  const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>row},storage:{downloadBounded:async()=>bytes}});
  assert.equal((await h(req())).status,503);
 }
 const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>true},manifest:{lookup:async()=>{throw Error('private credential detail');}},storage:{downloadBounded:async()=>jpeg}});
 assert.equal(await (await h(req())).text(),'Photo unavailable');
});

test('gateway requires durable budget adapter and reserves before consulting manifest',async()=>{
 for(const budget of [undefined,{reserve:async()=>false},{reserve:async()=>{throw Error('backend');}},{reserve:async()=>({allowed:true})}]){
  let calls=0;const h=createGalleryPhotoHandler({enabled:true,budget,manifest:{lookup:async()=>{calls++;return row;}},storage:{downloadBounded:async()=>jpeg}});
  const response=await h(req());assert.ok([429,503].includes(response.status));assert.equal(calls,0);
 }
});
test('failed or missing images retain full budget reservation and never request refunds',async()=>{
 let reservations=0;const h=createGalleryPhotoHandler({enabled:true,budget:{reserve:async()=>{reservations++;return true;}},manifest:{lookup:async()=>null},storage:{downloadBounded:async()=>jpeg}});
 assert.equal((await h(req())).status,404);assert.equal(reservations,1);
});
