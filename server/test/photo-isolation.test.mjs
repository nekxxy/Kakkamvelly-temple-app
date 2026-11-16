import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { PassThrough } from 'node:stream';
import sharp from 'sharp';
import { normalizeInSubprocess } from '../photo-isolation.mjs';
function fake(onInput){
 const child=new EventEmitter();child.stdin=new PassThrough();child.stdout=new PassThrough();
 child.killed=false;child.kill=signal=>{child.killed=signal;};
 child.stdin.on('finish',()=>onInput(child));return child;
}
const tiny=Buffer.from([255,216,255,217]);
test('subprocess strips metadata and returns actual normalized JPEG',async()=>{
 const image=await sharp({create:{width:10,height:12,channels:3,background:'#229955'}}).withMetadata({orientation:6}).png().toBuffer();
 const out=await normalizeInSubprocess(image);const meta=await sharp(out).metadata();
 assert.equal(meta.format,'jpeg');assert.equal(meta.width,12);assert.equal(meta.height,10);assert.equal(meta.exif,undefined);assert.equal(meta.orientation,undefined);
});
test('malformed image exits without returning a derivative',async()=>{
 await assert.rejects(normalizeInSubprocess(Buffer.from('not-an-image')),/Photo decoder failed/);
});
test('decoder inherits no environment, shell or caller arguments',async()=>{
 let options,args,exe;
 const spawnImpl=(e,a,o)=>{exe=e;args=a;options=o;return fake(c=>{c.stdout.emit('data',tiny);c.emit('close',0,null);});};
 process.env.PHOTO_TEST_SECRET='must-not-pass';
 try{assert.deepEqual(await normalizeInSubprocess(tiny,{spawnImpl}),tiny);}finally{delete process.env.PHOTO_TEST_SECRET;}
 assert.equal(exe,process.execPath);assert.equal(args.length,2);assert.ok(args[1].endsWith('/photo-decode-child.mjs'));assert.equal(options.shell,false);
 assert.deepEqual(options.env,{NODE_ENV:'production',UV_THREADPOOL_SIZE:'1',MALLOC_ARENA_MAX:'2'});
});
test('deadline kills stalled decoder and never returns partial bytes',async()=>{
 const child=fake(c=>c.stdout.emit('data',tiny));
 await assert.rejects(normalizeInSubprocess(tiny,{timeoutMs:10,spawnImpl:()=>child}),/Photo decoder failed/);assert.equal(child.killed,'SIGKILL');
});
test('oversized output kills decoder before buffering beyond limit',async()=>{
 const child=fake(c=>c.stdout.emit('data',Buffer.alloc(4194305)));
 await assert.rejects(normalizeInSubprocess(tiny,{spawnImpl:()=>child}),/Photo decoder failed/);assert.equal(child.killed,'SIGKILL');
});
test('non-JPEG and failed child never return a derivative',async()=>{
 for(const [bytes,code] of [[Buffer.from('invalid'),0],[tiny,1]]){
  await assert.rejects(normalizeInSubprocess(tiny,{spawnImpl:()=>fake(c=>{c.stdout.emit('data',bytes);c.emit('close',code,null);})}),/Photo decoder failed/);
 }
});
test('input caps reject before child spawn',async()=>{
 let calls=0;const spawnImpl=()=>{calls++;throw Error('must not spawn');};
 for(const input of [null,Buffer.alloc(0),Buffer.alloc(8388609)])await assert.rejects(normalizeInSubprocess(input,{spawnImpl}));
 await assert.rejects(normalizeInSubprocess(tiny,{timeoutMs:20001,spawnImpl}));assert.equal(calls,0);
});
