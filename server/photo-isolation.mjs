import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const childPath=fileURLToPath(new URL('./photo-decode-child.mjs',import.meta.url));
/** Separate credential-free process, bounded wall time and pipe sizes.
 * This is defense in depth, NOT an OS sandbox: heap flags do not cap native
 * memory or deny filesystem/network access. Deploy inside restricted CPU/RAM,
 * no-network, read-only-filesystem isolation before enabling uploads. */
export async function normalizeInSubprocess(input,{timeoutMs=20000,spawnImpl=spawn}={}){
 if(!Buffer.isBuffer(input)||input.length===0||input.length>8388608)throw Error('Invalid image size');
 if(!Number.isInteger(timeoutMs)||timeoutMs<1||timeoutMs>20000)throw Error('Invalid decoder deadline');
 return new Promise((resolve,reject)=>{
  let child,settled=false,size=0;const chunks=[];
  const clear=()=>{for(const b of chunks)b.fill(0);chunks.length=0;};
  let timer;
  function fail(){if(settled)return;settled=true;clearTimeout(timer);clear();child?.kill('SIGKILL');reject(Error('Photo decoder failed'));}
  try{
   child=spawnImpl(process.execPath,['--max-old-space-size=64',childPath],{
    shell:false,stdio:['pipe','pipe','ignore'],windowsHide:true,
    // Never inherit service credentials, NODE_OPTIONS, proxies or preload hooks.
    env:{NODE_ENV:'production',UV_THREADPOOL_SIZE:'1',MALLOC_ARENA_MAX:'2'}
   });
   timer=setTimeout(fail,timeoutMs);
   child.once('error',fail);child.stdin.on('error',fail);child.stdout.on('error',fail);
   child.stdout.on('data',data=>{
    if(settled)return;size+=data.length;
    if(size>4194304){fail();return;}chunks.push(Buffer.from(data));
   });
   child.once('close',(code,signal)=>{
    if(settled)return;
    if(code!==0||signal||size<4){fail();return;}
    const result=Buffer.concat(chunks);
    if(result[0]!==255||result[1]!==216||result.at(-2)!==255||result.at(-1)!==217){result.fill(0);fail();return;}
    settled=true;clearTimeout(timer);clear();resolve(result);
   });
   child.stdin.end(input);
  }catch{fail();}
 });
}
