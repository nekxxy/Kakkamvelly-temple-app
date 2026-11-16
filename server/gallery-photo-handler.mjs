const UUID='[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}';
const route=new RegExp(`^(?:/functions/v1)?/temple-gallery/(${UUID})/(${UUID})$`);
const headers={'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Cross-Origin-Resource-Policy':'cross-origin'};
const reply=(status,text='')=>new Response(text,{status,headers:{...headers,'Content-Type':'text/plain; charset=utf-8'}});
/** Prepared anonymous READ gateway for published derivatives only. No listener,
 * network adapter or deployment. Keep disabled until manifest finalization,
 * publication approval, hosting/egress controls and hosted checks are complete.
 * manifest.lookup must read CURRENT published state/config, never a stale cache.
 * storage.downloadBounded must enforce size/time caps while streaming. */
export function createGalleryPhotoHandler({enabled=false,manifest,storage}={}){
 function valid(x,asset,publication){
  return x?.asset_id===asset && x?.publication_id===publication && x.bucket==='temple-photo-gallery' &&
   new RegExp(`^${asset}/${publication}/${UUID}\\.jpg$`).test(x.path??'');
 }
 return async request=>{
  if(!enabled)return reply(404);
  let url;try{url=new URL(request.url);}catch{return reply(404);}
  const ids=route.exec(url.pathname);if(!ids||url.search)return reply(404);
  if(!['GET','HEAD'].includes(request.method))return new Response(null,{status:405,headers:{...headers,Allow:'GET, HEAD'}});
  if(!manifest?.lookup||!storage?.downloadBounded)return reply(503,'Photo unavailable');
  const [,asset,publication]=ids;
  try{
   const row=await manifest.lookup(asset,publication);
   if(!valid(row,asset,publication))return reply(404);
   // HEAD discloses neither internal path nor unverified byte length.
   if(request.method==='HEAD')return new Response(null,{status:200,headers:{...headers,'Content-Type':'image/jpeg'}});
   const bytes=await storage.downloadBounded(row.bucket,row.path,4194304);
   if(!Buffer.isBuffer(bytes)||bytes.length<4||bytes.length>4194304||bytes[0]!==255||bytes[1]!==216||bytes.at(-2)!==255||bytes.at(-1)!==217)return reply(503,'Photo unavailable');
   // Re-check after the fetch: a canceled/revoked manifest or disabled feature
   // during the download must not release already buffered private bytes.
   const current=await manifest.lookup(asset,publication);
   if(!valid(current,asset,publication)||current.path!==row.path)return reply(404);
   return new Response(bytes,{status:200,headers:{...headers,'Content-Type':'image/jpeg','Content-Length':String(bytes.length)}});
  }catch{return reply(503,'Photo unavailable');}
 };
   }
