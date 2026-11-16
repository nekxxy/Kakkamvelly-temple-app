import { supabasePhotoAdapters } from './photo-adapters.mjs';
const UUID='[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}';
/** Server-only, no network on construction. Never instantiate in a client app.
 * Service credentials remain here, not in the raster decoder or returned URL. */
export function supabasePhotoPublicationAdapters({url,serviceKey,fetchImpl=fetch}){
 const source=supabasePhotoAdapters({url,serviceKey,fetchImpl}); // validates fixed origin
 const headers={apikey:serviceKey,Authorization:`Bearer ${serviceKey}`};
 const targetPath=new RegExp(`^${UUID}/${UUID}/${UUID}\\.jpg$`);
 async function call(path,{method='POST',body,extra={}}={}){
  const r=await fetchImpl(path,{method,headers:{...headers,...extra},body,redirect:'error',signal:AbortSignal.timeout(5000)});
  if(!r.ok)throw Error('Photo publication unavailable');return r;
 }
 async function rpc(name,body){const r=await call(`${url}/rest/v1/rpc/${name}`,{body:JSON.stringify(body),extra:{'Content-Type':'application/json'}});return r.json();}
 return {
  publications:{claim:id=>rpc('claim_temple_photo_publication',{p_id:id}),finish:(id,token)=>rpc('finish_temple_photo_publication',{p_id:id,p_token:token})},
  budget:{reserve:()=>rpc('reserve_temple_gallery_delivery',{})},
  manifest:{lookup:(asset,id)=>rpc('get_temple_gallery_photo_manifest',{p_asset_id:asset,p_publication_id:id})},
  storage:{
   async downloadBounded(bucket,path,max){
    if(bucket==='temple-photo-quarantine')return source.storage.downloadBounded(bucket,path,max);
    if(bucket!=='temple-photo-gallery'||!targetPath.test(path)||!Number.isSafeInteger(max)||max<1||max>4194304)throw Error('Invalid derivative request');
    const r=await call(`${url}/storage/v1/object/temple-photo-gallery/${path}`,{method:'GET'});
    if(Number(r.headers.get('content-length'))>max){await r.body?.cancel();throw Error('Derivative too large');}
    const reader=r.body?.getReader();if(!reader)throw Error('Missing derivative');
    let size=0;const chunks=[];
    try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>max)throw Error('Derivative too large');chunks.push(Buffer.from(value));}return Buffer.concat(chunks);}
    finally{for(const b of chunks)b.fill(0);await reader.cancel();}
   },
   async insert(bucket,path,bytes,type){
    if(bucket!=='temple-photo-gallery'||!targetPath.test(path)||!Buffer.isBuffer(bytes)||bytes.length<4||bytes.length>4194304||type!=='image/jpeg')throw Error('Invalid derivative');
    await call(`${url}/storage/v1/object/temple-photo-gallery/${path}`,{body:bytes,extra:{'Content-Type':'image/jpeg','x-upsert':'false','Cache-Control':'private, no-store'}});
   }
  }
 };
}
