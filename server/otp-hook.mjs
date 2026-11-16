import { createHmac } from 'node:crypto';
import { Webhook } from 'standardwebhooks';
const reply=(status,message)=>new Response(JSON.stringify(message?{error:{http_code:status,message}}:{}),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
const digest=(key,domain,text)=>createHmac('sha256',key).update(domain+'\0'+text).digest('hex');
async function rawBody(request){
  const reader=request.body?.getReader();if(!reader)throw Error('body');
  const parts=[];let size=0;
  try {while(true){const {done,value}=await reader.read();if(done)break;size+=value.byteLength;if(size>200*1024)throw Error('large');parts.push(Buffer.from(value));}}
  finally{await reader.cancel();}
  return Buffer.concat(parts).toString('utf8');
}
/** Server-only request handler. No credentials, deployment or provider enabled
 * by importing this module. Adapters are injected; never accept them from HTTP.
 * Reservation RPC must COMMIT before sender invocation. No automatic retry. */
export function createOtpHook({enabled=false,hookSecret,dedupeSecret,ledger,sender}={}){
  return async request=>{
    if(request.method!=='POST')return reply(405,'Method not allowed');
    if(!enabled)return reply(503,'Login delivery is not enabled');
    if(!hookSecret||!dedupeSecret||dedupeSecret.length<32||!ledger||!sender)return reply(503,'Login delivery unavailable');
    let data,raw;
    try{
      raw=await rawBody(request);
      const headers=Object.fromEntries(request.headers);
      if(!/^\d{10,12}$/.test(headers['webhook-timestamp']??''))throw Error('timestamp');
      data=new Webhook(hookSecret.replace(/^v1,whsec_/,'' )).verify(raw,headers);
    }catch{return reply(400,'Invalid request');}
    const phone=data?.sms?.phone??data?.user?.phone;
    // Phone-change delivery is held rather than silently choosing one identity.
    if(!/^\+[1-9][0-9]{6,14}$/.test(phone??'')||!/^\d{6}$/.test(data?.sms?.otp??'')||
       (data?.sms?.phone&&data?.user?.phone&&data.sms.phone!==data.user.phone))return reply(400,'Invalid request');
    // Stable across webhook-id/timestamp retries and JSON reformatting. Do not
    // use an unkeyed hash of the small OTP space. Key rotation needs overlap.
    const delivery=digest(dedupeSecret,'delivery-v1',JSON.stringify([phone,data.sms.otp]));
    const recipient=digest(dedupeSecret,'recipient-v1',phone);
    let reservation;
    try{reservation=await ledger.reserve(delivery,recipient);}catch{return reply(503,'Login delivery unavailable');}
    if(reservation?.status!=='reserved'||!reservation.id)return reply(400,'Login delivery unavailable');
    let outcome='unknown';
    let timer; const controller=new AbortController();
    try{
      const timeout=new Promise((_,reject)=>{timer=setTimeout(()=>{controller.abort();reject(Error('timeout'));},1800);});
      if((await Promise.race([sender.send({phone,otp:data.sms.otp,signal:controller.signal}),timeout]))?.accepted===true)outcome='accepted';
    }catch{}finally{clearTimeout(timer);}
    // Do not refund unknown outcomes. Even a timed-out call may be billable.
    try{await ledger.record(reservation.id,outcome);}catch{return reply(500,'Login delivery could not be confirmed');}
    return outcome==='accepted'?reply(200):reply(400,'Login delivery could not be confirmed');
  };
}
/** Fixed, HTTPS Supabase server endpoint. No URL or secret comes from a caller.
 * RPCs enforce price freshness, owner destination allowlist and all budgets. */
export function supabaseLedger({url,serviceKey,fetchImpl=fetch}){
  if(!/^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(url)||!serviceKey)throw Error('Invalid server configuration');
  async function rpc(name,body){
    const response=await fetchImpl(`${url}/rest/v1/rpc/${name}`,{method:'POST',redirect:'error',signal:AbortSignal.timeout(800),headers:{apikey:serviceKey,Authorization:`Bearer ${serviceKey}`,'Content-Type':'application/json'},body:JSON.stringify(body)});
    if(!response.ok)throw Error('Ledger unavailable');
    return response.status===204?null:response.json();
  }
  return {reserve:(d,r)=>rpc('reserve_visitor_otp',{p_delivery_hmac:d,p_recipient_hmac:r}),record:(id,outcome)=>rpc('record_visitor_otp',{p_id:id,p_outcome:outcome})};
      }
