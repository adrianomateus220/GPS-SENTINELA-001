const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const source=fs.readFileSync('app/src/main/assets/app.js','utf8');
async function run(answer,after){
 const elements=new Map();const el=id=>{if(!elements.has(id))elements.set(id,{textContent:'',value:'',style:{}});return elements.get(id)};
 const context=vm.createContext({console,AbortController,URL,Date,Math,Number,String,Promise,location:{hostname:'appassets.androidplatform.net'},navigator:{geolocation:{watchPosition:()=>1}},document:{getElementById:el,createElement:()=>({}),head:{append(){}},addEventListener(){}},window:{addEventListener(){}},setInterval(){},setTimeout(){return 1},clearTimeout(){},fetch:async()=>({ok:true,json:async()=>{if(after)vm.runInContext(after,context);return answer}})});
 vm.runInContext(source,context);
 vm.runInContext("lastFix={timestamp:Date.now(),coords:{latitude:-23.53,longitude:-46.79,accuracy:10}};addressPos=[-23.53,-46.79];addressAt=Date.now();address='Rua Teste • Número não disponível';$('houseNumber').textContent='Não disponível';$('district').textContent='Centro';",context);
 await vm.runInContext("enrichNumber([-23.53,-46.79],generation,'Rua Teste',addressAt)",context);
 return el('houseNumber').textContent;
}
(async()=>{
 const good={road:'R. Teste',house_number:'120',lat:-23.53,lon:-46.79};
 assert.equal(await run(good),'120');
 for(const bad of [{...good,road:'Rua Paralela'},{...good,lat:-23.54},{...good,house_number:null},{...good,lat:undefined}])assert.equal(await run(bad),'Não disponível');
 for(const change of ['generation++','lastFix.coords.latitude=-23.54','lastFix.coords.accuracy=100','addressAt++'])assert.equal(await run(good,change),'Não disponível');
 console.log('OK: fallback numeral, same street, distance, missing number, missing coordinates, stale response, movement and GPS accuracy.');
})().catch(e=>{console.error(e);process.exitCode=1});
