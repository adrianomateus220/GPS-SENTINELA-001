const assert=require('node:assert/strict'),R=require('../app/src/main/assets/road-ahead.js');
const way=(id,name,nodes,coords,highway='residential')=>({type:'way',id,tags:{name,highway},nodes,geometry:coords.map(([lon,lat])=>({lon,lat}))});
const a=way(1,'Rua A',[1,2,3],[[-.003,0],[0,0],[.003,0]]),b=way(2,'Av B',[4,3,5],[[.003,-.003],[.003,0],[.003,.003]],'primary'),p={lat:0,lon:0,accuracy:5},g=R.graph([a,b]);
assert.equal(R.infer(g,p,90,'Rua A').target,'Av B');
for(const [graph,point,heading,name] of [[g,p,270],[g,{...p,accuracy:80},90],[g,p,90,'Outra'],[R.graph([a,{...b,nodes:[40,30,50]}]),p,90],[R.graph([a,b,way(3,'Paralela',[7,8],[[0,.00002],[.003,.00002]])]),p,90],[R.graph([{...a,tags:{...a.tags,oneway:'-1'}},b]),p,90]])assert.equal(R.infer(graph,point,heading,name).target,undefined);
const now=Date.now(),start={lat:0,lon:0,accuracy:5,time:now-3000},end={lat:0,lon:.001,accuracy:5,time:now};
assert.equal(R.movement([start,end],now),90);assert.equal(R.movement([start,{...end,lon:.00001}],now),null);assert.equal(R.movement([start,{...end,heading:270}],now),null);
const areas=JSON.parse(require('node:fs').readFileSync('app/src/main/assets/areas.geojson','utf8'));assert.equal(areas.features.length,163);for(const f of areas.features)for(const poly of f.geometry.coordinates)for(const ring of poly){assert(ring.length>=4);assert.deepEqual(ring[0],ring.at(-1));}
console.log('Sentido, GPS, paralelas, viaduto, mão única e 163 limites: OK');
