import { test } from 'node:test';import assert from 'node:assert/strict';import { processPhoto, cleanupPhotoOriginals } from '../photo-worker.mjs';
const id='00000000-0000-0000-0000-000000000099';
const lease='00000000-0000-0000-0000-000000000055';
function f(){const calls=[];return{calls,enabled:true,id,assets:{claim:async()=>({id,lease_token:lease,output_path:`processed/${id}/${lease}.jpg`,bucket:'temple-photo-quarantine',path:`00000000-0000-0000-0000-000000000001/${id}/original`}),finish:async()=>calls.push('finish')},storage:{downloadBounded:async()=>{calls.push('download');return Buffer.from('fixture')},insert:async()=>calls.push('insert'),remove:async()=>calls.push('remove')},normalize:async()=>{calls.push('normalize');return Buffer.from('normalized')}};}
test('worker disabled by default',async()=>assert.equal((await processPhoto()).status,'disabled'));
test('untrusted path never reaches storage',async()=>{const v=f();v.assets.claim=async()=>({id,bucket:'temple-photo-quarantine',path:'../../other'});assert.equal((await processPhoto(v)).status,'not_pending');assert.deepEqual(v.calls,[]);});
test('processing order is normalize, insert, finish, then remove original',async()=>{const v=f();assert.equal((await processPhoto(v)).status,'processed');assert.deepEqual(v.calls,['download','normalize','insert','finish','remove']);});
test('decode failure never asserts processing or deletes original',async()=>{const v=f();v.normalize=async()=>{throw Error('decode')};assert.equal((await processPhoto(v)).status,'processing_unconfirmed');assert.deepEqual(v.calls,['download']);});
test('insert conflict never overwrites or finishes',async()=>{const v=f();v.storage.insert=async()=>{throw Error('conflict')};assert.equal((await processPhoto(v)).status,'processing_unconfirmed');assert.deepEqual(v.calls,['download','normalize']);});
test('finish failure keeps original and reports uncertain private artifact',async()=>{const v=f();v.assets.finish=async()=>{throw Error('db')};assert.equal((await processPhoto(v)).status,'processing_unconfirmed');assert.deepEqual(v.calls,['download','normalize','insert']);});
test('cleanup failure stays private and observable',async()=>{const v=f();v.storage.remove=async()=>{throw Error('storage')};assert.equal((await processPhoto(v)).status,'processed_cleanup_pending');});

test('cleanup default never deletes',async()=>assert.equal((await cleanupPhotoOriginals()).status,'disabled'));
test('cleanup validates entire batch before first delete',async()=>{
 const calls=[];const a=f();a.assets.cleanupBatch=async()=>[{id,bucket:'temple-photo-quarantine',path:'../other'}];a.assets.confirmErased=async()=>calls.push('confirmed');a.storage.remove=async()=>calls.push('deleted');
 assert.equal((await cleanupPhotoOriginals(a)).status,'invalid_batch');assert.deepEqual(calls,[]);
});
test('cleanup confirms only after successful deletion and server verification',async()=>{
 const a=f();a.assets.cleanupBatch=async()=>[{id,bucket:'temple-photo-quarantine',path:`00000000-0000-0000-0000-000000000001/${id}/original`}];a.assets.confirmErased=async()=>a.calls.push('confirmed');
 assert.deepEqual(await cleanupPhotoOriginals(a),{status:'complete',confirmed:1,pending:0});assert.deepEqual(a.calls,['remove','confirmed']);
});
test('cleanup uncertainty stays pending without false confirmation',async()=>{
 const a=f();a.assets.cleanupBatch=async()=>[{id,bucket:'temple-photo-quarantine',path:`00000000-0000-0000-0000-000000000001/${id}/original`}];a.assets.confirmErased=async()=>{throw Error('still exists')};
 assert.deepEqual(await cleanupPhotoOriginals(a),{status:'cleanup_pending',confirmed:0,pending:1});
});

test('missing worker lease never reaches storage',async()=>{const v=f();const claim=v.assets.claim;v.assets.claim=async()=>({...await claim(),lease_token:null});assert.equal((await processPhoto(v)).status,'not_pending');assert.deepEqual(v.calls,[]);});
