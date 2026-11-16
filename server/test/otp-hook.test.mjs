import { test } from 'node:test';import assert from 'node:assert/strict';import { Webhook } from 'standardwebhooks';
import { createOtpHook,supabaseLedger } from '../otp-hook.mjs';
const secret=Buffer.from('test-fixture-not-a-real-secret-key').toString('base64');
const body=JSON.stringify({user:{phone:'+15555550123'},sms:{otp:'123456'}});
function request(payload=body,id='test-1',date=new Date()){
 const headers={'webhook-id':id,'webhook-timestamp':String(Math.floor(date.getTime()/1000)),'webhook-signature':new Webhook(secret).sign(id,date,payload)};
 return new Request('https://test.invalid/hook',{method:'POST',headers,body:payload});
}
function fixture(){const calls={sent:0,reserved:[],recorded:[]};return{calls,enabled:true,hookSecret:'v1,whsec_'+secret,dedupeSecret:'fixture-key-not-real-'.repeat(3),ledger:{reserve:async(d,r)=>{calls.reserved.push([d,r]);return{status:'reserved',id:'test-id'};},record:async(...v)=>calls.recorded.push(v)},sender:{send:async()=>{calls.sent++;return{accepted:true};}}};}
test('default handler makes no calls',async()=>assert.equal((await createOtpHook()(request())).status,503));
test('signature checked before any reservation',async()=>{const f=fixture();const r=request();r.headers.set('webhook-signature','v1,invalid');assert.equal((await createOtpHook(f)(r)).status,400);assert.equal(f.calls.reserved.length,0);});
test('stale signed message rejected',async()=>{const f=fixture();assert.equal((await createOtpHook(f)(request(body,'old',new Date(1)))).status,400);assert.equal(f.calls.sent,0);});
test('signed malformed phone change rejected',async()=>{const f=fixture();const payload=JSON.stringify({user:{phone:'+15555550123'},sms:{phone:'+15555550124',otp:'123456'}});assert.equal((await createOtpHook(f)(request(payload))).status,400);assert.equal(f.calls.sent,0);});
test('changed retry headers do not change private delivery fingerprint',async()=>{const f=fixture();const h=createOtpHook(f);await h(request(body,'one'));await h(request(body,'two'));assert.deepEqual(f.calls.reserved[0],f.calls.reserved[1]);assert.match(f.calls.reserved[0][0],/^[a-f0-9]{64}$/);});
test('duplicate reservation never calls provider',async()=>{const f=fixture();f.ledger.reserve=async()=>({status:'duplicate'});assert.equal((await createOtpHook(f)(request())).status,400);assert.equal(f.calls.sent,0);});
test('ledger outage never calls provider',async()=>{const f=fixture();f.ledger.reserve=async()=>{throw Error('private')};assert.equal((await createOtpHook(f)(request())).status,503);assert.equal(f.calls.sent,0);});
test('provider timeout retains unknown outcome and no retry',async()=>{const f=fixture();f.sender.send=async()=>{f.calls.sent++;throw Error('timeout private phone')};const r=await createOtpHook(f)(request());assert.equal(r.status,400);assert.equal(f.calls.sent,1);assert.deepEqual(f.calls.recorded,[['test-id','unknown']]);assert.equal((await r.text()).includes('private phone'),false);assert.equal(r.headers.has('retry-after'),false);});
test('provider acceptance only then returns success',async()=>{const f=fixture();assert.equal((await createOtpHook(f)(request())).status,200);assert.deepEqual(f.calls.recorded,[['test-id','accepted']]);});
test('record failure cannot claim confirmed success',async()=>{const f=fixture();f.ledger.record=async()=>{throw Error('db')};assert.equal((await createOtpHook(f)(request())).status,500);});
test('oversized signed body rejected before reserve',async()=>{const f=fixture();assert.equal((await createOtpHook(f)(request(' '.repeat(205000)+body))).status,400);assert.equal(f.calls.reserved.length,0);});
test('ledger adapter is fixed-origin and bounded',async()=>{assert.throws(()=>supabaseLedger({url:'http://bad.invalid',serviceKey:'fixture'}));const calls=[];const l=supabaseLedger({url:'https://test.supabase.co',serviceKey:'fixture',fetchImpl:async(u,o)=>{calls.push([u,o]);return new Response(JSON.stringify({status:'reserved'}),{status:200});}});await l.reserve('a','b');assert.equal(calls[0][0],'https://test.supabase.co/rest/v1/rpc/reserve_visitor_otp');assert.equal(calls[0][1].redirect,'error');assert.ok(calls[0][1].signal);});
