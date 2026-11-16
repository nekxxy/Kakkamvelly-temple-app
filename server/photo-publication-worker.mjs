const UUID=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
/** Disabled publication worker. Copies ONLY an already reviewed normalized JPEG
 * into a second PRIVATE bucket. SQL finalization is the sole manifest switch.
 * Never retries an uncertain upload/finalization automatically. */
export async function publishPhoto({enabled=false,id,publications,storage}={}){
 if(!enabled)return{status:'disabled'};
 if(!UUID.test(id??'')||!publications?.claim||!publications?.finish||!storage?.downloadBounded||!storage?.insert)return{status:'invalid'};
 let claim;
 try{claim=await publications.claim(id);}catch{return{status:'unavailable'};}
 if(claim?.id!==id||!UUID.test(claim.asset_id??'')||!UUID.test(claim.lease_token??'')||
  claim.source_bucket!=='temple-photo-quarantine'||claim.target_bucket!=='temple-photo-gallery'||
  !(new RegExp(`^processed/${claim.asset_id}(/[0-9a-f-]{36})?\\.jpg$`)).test(claim.source_path??'')||
  claim.target_path!==`${claim.asset_id}/${id}/${claim.lease_token}.jpg`)return{status:'not_queued'};
 let bytes;
 try{
  bytes=await storage.downloadBounded(claim.source_bucket,claim.source_path,4194304);
  if(!Buffer.isBuffer(bytes)||bytes.length<4||bytes.length>4194304||bytes[0]!==255||bytes[1]!==216||bytes.at(-2)!==255||bytes.at(-1)!==217)throw Error('Invalid derivative');
  await storage.insert(claim.target_bucket,claim.target_path,bytes,'image/jpeg');
  const revision=await publications.finish(id,claim.lease_token);
  if(!Number.isSafeInteger(revision)||revision<1)throw Error('Invalid revision receipt');
  return{status:'published',revision};
 }catch{return{status:'publication_unconfirmed'};}
 finally{if(Buffer.isBuffer(bytes))bytes.fill(0);}
}
