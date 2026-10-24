const {chromium}=require(process.env.PLAYWRIGHT_MODULE),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 try{
 const page=await browser.newPage({viewport:{width:393,height:852}}),errors=[],requests=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>requests.push(r.url()));
 // Simulate an unavailable map: GPS must start independently.
 await page.route('https://unpkg.com/**',r=>r.abort());
 let delayed=null;
 await page.route('https://nominatim.openstreetmap.org/**',async r=>{
  const lat=Number(new URL(r.request().url()).searchParams.get('lat'));
  if(lat===-23.532){delayed=r;return;}
  await r.fulfill({json:{address:lat===-23.533?{road:'Rua Nova',suburb:'Bairro Novo'}:{road:'Rua de Teste',house_number:'120',suburb:'Centro',city:'Osasco',state:'São Paulo'}}});
 });
 await page.addInitScript(()=>{
  window.watches=0;window.clears=0;
  window.fix=(lat=-23.53)=>({timestamp:Date.now(),coords:{latitude:lat,longitude:-46.79,accuracy:15}});
  Object.defineProperty(navigator,'geolocation',{value:{watchPosition:(ok,fail)=>{window.watches++;window.sendFix=ok;window.failGPS=fail;setTimeout(()=>ok(window.fix()),0);return window.watches;},clearWatch:()=>window.clears++}});
 });
 await page.goto('http://127.0.0.1:8765');
 await page.waitForFunction(()=>document.querySelector('#road').textContent==='Rua de Teste');
 assert.equal(await page.evaluate(()=>window.watches),1);
 await page.evaluate(()=>{updateHeading({timestamp:Date.now(),coords:{latitude:-23.53,longitude:-46.79,accuracy:10,heading:90,speed:4}});});
 assert.equal(await page.evaluate(()=>travelHeading),90);
 assert.equal(await page.locator('#houseNumber').innerText(),'120');assert.equal(await page.locator('#district').innerText(),'Centro');
 assert.equal(await page.locator('#start,#direction,#emergency,#emergencyPanel').count(),0);
 assert(!requests.some(u=>/areas.geojson|hospital|overpass|osrm|road-ahead/.test(u)));
 await page.evaluate(()=>window.sendFix(window.fix(-23.532)));
 await page.waitForFunction(()=>geoBusy);
 await page.waitForTimeout(100);
 assert(delayed,'second address request started');
 await page.evaluate(()=>window.sendFix(window.fix(-23.533)));
 await delayed.fulfill({json:{address:{road:'Rua Atrasada',house_number:'999',suburb:'Antigo'}}});
 await page.waitForFunction(()=>document.querySelector('#road').textContent==='Rua Nova');
 assert.equal(await page.locator('#houseNumber').innerText(),'Não disponível');
 assert.equal(await page.locator('#district').innerText(),'Bairro Novo');
 assert(!(await page.locator('body').innerText()).includes('Rua Atrasada'));
 await page.evaluate(()=>{lastFix.timestamp=Date.now()-20000;tick();});
 assert.equal(await page.locator('#houseNumber').innerText(),'Não disponível');
 assert.equal(await page.locator('#district').innerText(),'—');
 await page.click('#pause');assert.equal(await page.locator('#pause').innerText(),'Retomar localização');
 await page.click('#pause');await page.waitForFunction(()=>window.watches===2);
 await page.evaluate(()=>window.failGPS({code:1}));
 assert((await page.locator('#status').innerText()).includes('Permita'));
 assert.equal(await page.locator('#pause').innerText(),'Tentar localização');
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true);assert.deepEqual(errors,[]);
 console.log('OK: auto-start, offline map, removed modules, latest fix, unavailable number, stale address, pause/resume, denied permission and mobile layout.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
