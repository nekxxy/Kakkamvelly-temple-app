import { normalizePhoto } from './photo-normalize.mjs';
const UUID=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
/** Server-only worker operation. Adapters may only use fixed Supabase origin
 * and service credentials. Run decoder in an isolated restricted process.
 * There is no default network adapter, listener, deployment or upload enable.
 * Never accepts an object path or URL from the upload caller. */
export async function processPhoto({enabled=false,id,assets,storage,normalize=normalizePhoto}={}){
 if(!enabled)return{status:'disabled'};
 if(!UUID.test(id??'')||!assets||!storage)return{status:'invalid'};
 let slot;
 try{slot=await assets.pending(id);}catch{return{status:'unavailable'};}
 const expected=new RegExp(`^[0-9a-f-]{36}/${id}/original$`);
 if(slot?.id!==id||slot.bucket!=='temple-photo-quarantine'||!expected.test(slot.path??''))return{status:'not_pending'};
 const outputPath=`processed/${id}.jpg`;
 let input,output;
 try{
  // Adapter must enforce byte/time caps during download, not after buffering.
  input=await storage.downloadBounded(slot.bucket,slot.path,8*1024*1024);
  output=await normalize(input);
  // insert-only: immutable output. Never overwrite on retry/race.
  await storage.insert(slot.bucket,outputPath,output,'image/jpeg');
  await assets.finish(id,outputPath);
 }catch{return{status:'processing_unconfirmed'};}
 finally{if(Buffer.isBuffer(input))input.fill(0);if(Buffer.isBuffer(output))output.fill(0);}
 // Private original deletion is separate. Failure does not roll back processed
 // state or expose the object; report it for the retention reconciler.
 try{await storage.remove(slot.bucket,slot.path);}catch{return{status:'processed_cleanup_pending'};}
 return{status:'processed'}; // never reviewed, published or publicly accessible
}

/** Retryable private ORIGINAL cleanup only after database processing assertion.
 * Does not reclaim slots or delete derivatives/asset rows. A crash after delete
 * is safe: next pass repeats the exact idempotent delete then verifies absence. */
export async function cleanupPhotoOriginals({enabled=false,assets,storage}={}){
 if(!enabled)return{status:'disabled',confirmed:0,pending:0};
 if(!assets?.cleanupBatch||!assets?.confirmErased||!storage?.remove)return{status:'invalid',confirmed:0,pending:0};
 let rows;try{rows=await assets.cleanupBatch();}catch{return{status:'unavailable',confirmed:0,pending:0};}
 if(!Array.isArray(rows)||rows.length>24)return{status:'invalid_batch',confirmed:0,pending:0};
 // Validate whole batch before any side effect. No paths from an HTTP caller.
 if(rows.some(x=>!UUID.test(x?.id??'')||x.bucket!=='temple-photo-quarantine'||
   !(new RegExp(`^[0-9a-f-]{36}/${x.id}/original$`)).test(x.path??'')))return{status:'invalid_batch',confirmed:0,pending:0};
 let confirmed=0,pending=0;
 for(const x of rows){
  try{await storage.remove(x.bucket,x.path);await assets.confirmErased(x.id);confirmed++;}
  catch{pending++;} // No claim of erasure while storage or verification failed.
 }
 return{status:pending?'cleanup_pending':'complete',confirmed,pending};
                                                                           }
