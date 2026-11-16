import sharp from 'sharp';
import { normalizePhoto } from './photo-normalize.mjs';
// No service credentials, URLs, user paths or arguments. One image per process.
sharp.cache(false);sharp.concurrency(1);
let size=0,input,output;const chunks=[];
try{
 for await(const chunk of process.stdin){
  size+=chunk.length;if(size>8388608)throw Error('Input too large');chunks.push(chunk);
 }
 input=Buffer.concat(chunks);for(const b of chunks)b.fill(0);chunks.length=0;
 output=await normalizePhoto(input);
 await new Promise((resolve,reject)=>process.stdout.write(output,e=>e?reject(e):resolve()));
}catch{process.exitCode=1;}
finally{input?.fill(0);output?.fill(0);for(const b of chunks)b.fill(0);}
