/** Server-only Supabase adapters. Caller must isolate raster worker and keep
 * service credentials out of mobile/client bundles. No network on construction. */
export function supabasePhotoAdapters({url,serviceKey,fetchImpl=fetch}){
 if(!/^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(url)||!serviceKey)throw Error('Invalid server configuration');
 const headers={apikey:serviceKey,Authorization:`Bearer ${serviceKey}`};
 const bucket='temple-photo-quarantine';
 function objectPath(b,p){
  if(b!==bucket||!(/^[0-9a-f-]{36}\/[0-9a-f-]{36}\/original$/.test(p)||/^processed\/[0-9a-f-]{36}\.jpg$/.test(p)))throw Error('Invalid object path');
  return `${url}/storage/v1/object/${bucket}/${p}`;
 }
 async function call(path,{method='POST',body,extra={}}={}){
  const r=await fetchImpl(path,{method,headers:{...headers,...extra},body,redirect:'error',signal:AbortSignal.timeout(5000)});
  if(!r.ok)throw Error('Private photo storage unavailable');return r;
 }
 async function rpc(name,body){const r=await call(`${url}/rest/v1/rpc/${name}`,{body:JSON.stringify(body),extra:{'Content-Type':'application/json'}});return r.status===204?null:r.json();}
 return {
  assets:{pending:id=>rpc('get_pending_temple_photo',{p_id:id}),finish:(id,path)=>rpc('finish_temple_photo_processing',{p_id:id,p_normalized_path:path})},
  storage:{
   async downloadBounded(b,p,max){
    if(!Number.isSafeInteger(max)||max<=0||max>8388608)throw Error('Invalid size limit');
    const r=await call(objectPath(b,p),{method:'GET'});
    if(Number(r.headers.get('content-length'))>max) {await r.body?.cancel();throw Error('Image too large');}
    const reader=r.body?.getReader();if(!reader)throw Error('Missing image');
    let size=0;const chunks=[];
    try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>max)throw Error('Image too large');chunks.push(Buffer.from(value));}}
    finally{await reader.cancel();}
    return Buffer.concat(chunks);
   },
   async insert(b,p,bytes,type){
    if(!p.startsWith('processed/')||!Buffer.isBuffer(bytes)||bytes.length>4194304||type!=='image/jpeg')throw Error('Invalid derivative');
    await call(objectPath(b,p),{body:bytes,extra:{'Content-Type':'image/jpeg','x-upsert':'false','Cache-Control':'private, no-store'}});
   },
   async remove(b,p){
    objectPath(b,p); // validate before constructing delete request
    if(!p.endsWith('/original'))throw Error('Original deletion only');
    await call(`${url}/storage/v1/object/${bucket}`,{method:'DELETE',body:JSON.stringify({prefixes:[p]}),extra:{'Content-Type':'application/json'}});
   }
  }
 };
}
