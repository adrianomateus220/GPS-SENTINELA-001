const {chromium}=require(process.env.PLAYWRIGHT_MODULE),assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:852}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://unpkg.com/**',r=>r.abort());
 await page.route('https://nominatim.openstreetmap.org/**',r=>r.fulfill({json:{address:{road:'Rua de Teste',house_number:'120',suburb:'Centro',city:'Osasco',state:'São Paulo'}}}));
 await page.route('https://overpass-api.de/**',r=>r.fulfill({json:{elements:[{type:'node',id:999,lat:-23.531,lon:-46.79,tags:{amenity:'hospital',name:'Hospital Teste sem comprovação'}},{type:'node',id:998,lat:-23.531,lon:-46.79,tags:{amenity:'hospital',name:'UPA Teste'}}]}}));
 await page.route('https://router.project-osrm.org/**',r=>{const coords=r.request().url().split('/driving/')[1].split('?')[0].split(';'),n=coords.length;return r.fulfill({json:{code:'Ok',distances:[coords.map((_,i)=>i?(n-i)*1000:0)],durations:[coords.map((_,i)=>i?600:0)],sources:[{distance:0}],destinations:coords.map(()=>({distance:0}))}});});
 await page.addInitScript(()=>{const fix=()=>({timestamp:Date.now(),coords:{latitude:-23.53,longitude:-46.79,accuracy:15,speed:0,heading:null}});Object.defineProperty(navigator,'geolocation',{value:{getCurrentPosition:ok=>ok(fix()),watchPosition:ok=>{ok(fix());return 1},clearWatch:()=>{}}});});
 await page.goto('http://127.0.0.1:8765');await page.waitForFunction(()=>typeof hospitals!=='undefined'&&hospitals.length===16);await page.click('#emergency');
 await page.waitForFunction(()=>document.querySelector('#nearbyStatus').textContent.includes('Busca online concluída'));
 assert((await page.locator('#houseNumber').innerText()).includes('120'));assert.equal(await page.locator('#accuracy,#updated,#speed').count(),0);
 assert(await page.locator('#hospitals .hospital').count()>1);assert.equal(await page.locator('#otherHospitals .hospital').count(),1);await page.getByText('Outros hospitais próximos • capacidade a confirmar',{exact:true}).click();assert.equal(await page.locator('#otherHospitals button').innerText(),'REGULAÇÃO CONFIRMOU ESTE HOSPITAL');
 const distances=await page.locator('#hospitals .hospital .distance').allTextContents();const kms=distances.filter(s=>s.includes('km pelo')).map(s=>parseFloat(s));assert.deepEqual(kms,[...kms].sort((a,b)=>a-b));assert(kms.length>1);
 await page.fill('#filterHospital','zzzzz');assert.equal(await page.locator('#hospitals .hospital').count(),0);await page.fill('#filterHospital','');
 await page.evaluate(()=>{lastFix.timestamp=0;renderHospitals();});assert.equal(await page.locator('#hospitals .hospital').count(),0);assert((await page.locator('#nearbyStatus').innerText()).includes('Aguardando'));
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true);assert.deepEqual(errors,[]);await browser.close();console.log('UI: campos removidos, busca, ordem dos trajetos, capacidade não confirmada, filtros e GPS antigo: OK');
})();
