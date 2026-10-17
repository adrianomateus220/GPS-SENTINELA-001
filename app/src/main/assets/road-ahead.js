/* Geometric, conservative road-graph inference. Not turn-by-turn navigation. */
'use strict';
const RoadAhead=(()=>{
 const R=Math.PI/180;
 const norm=s=>String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
 const delta=(a,b)=>Math.abs(((a-b+540)%360)-180);
 function xy(p,origin){return [(p.lon-origin.lon)*111320*Math.cos(origin.lat*R),(p.lat-origin.lat)*111320];}
 function bearing(a,b){const v=xy(b,a);return (Math.atan2(v[0],v[1])/R+360)%360;}
 function distance(a,b){const v=xy(b,a);return Math.hypot(...v);}
 function project(p,a,b){const av=xy(a,p),bv=xy(b,p),v=[bv[0]-av[0],bv[1]-av[1]],len=v[0]**2+v[1]**2;if(!len)return {d:Infinity,t:0};const t=Math.max(0,Math.min(1,-(av[0]*v[0]+av[1]*v[1])/len));return {d:Math.hypot(av[0]+t*v[0],av[1]+t*v[1]),t};}
 function movement(history,now=Date.now()){
  const h=history.filter(p=>now-p.time<=20000&&p.accuracy<=35);if(h.length<2)return null;
  const b=h[h.length-1];if(now-b.time>8000)return null;
  let a=null;for(let i=h.length-2;i>=0;i--){if(distance(h[i],b)>=Math.max(15,h[i].accuracy+b.accuracy)){a=h[i];break;}}
  if(!a||b.time-a.time>15000)return null;
  const dir=bearing(a,b);
  if(Number.isFinite(b.heading)&&delta(dir,b.heading)>45)return null;
  return dir;
 }
 function graph(elements){
  const ways=elements.filter(w=>w.type==='way'&&w.tags&&w.tags.highway&&w.nodes?.length===w.geometry?.length&&!['private','no'].includes(w.tags.access));
  const adjacent=new Map();
  for(const w of ways)for(const n of w.nodes){if(!adjacent.has(n))adjacent.set(n,[]);adjacent.get(n).push(w);}
  return {ways,adjacent};
 }
 function allowed(w,sign){const one=w.tags.oneway;return one==='-1'?sign<0:['yes','1','true'].includes(one)||(w.tags.junction==='roundabout'&&one!=='no')?sign>0:true;}
 function infer(g,p,heading,roadName){
  if(!g||!Number.isFinite(heading)||p.accuracy>35)return {message:'Aguardando deslocamento e GPS preciso'};
  const candidates=[];
  for(const w of g.ways){if(!w.tags.name&&!w.tags.ref)continue;for(let i=0;i<w.geometry.length-1;i++){
   const a=w.geometry[i],b=w.geometry[i+1],pr=project(p,a,b);if(pr.d>Math.max(12,p.accuracy))continue;
   for(const sign of [1,-1]){const turn=delta(heading,sign===1?bearing(a,b):bearing(b,a));if(turn>40||!allowed(w,sign))continue;candidates.push({w,i,sign,pr,turn,score:pr.d+turn/3});}
  }}
  candidates.sort((a,b)=>a.score-b.score);if(!candidates.length)return {message:'Sentido não identificado na malha viária'};
  const c=candidates[0];
  // Parallel ways inside GPS uncertainty: never choose one only because the name matches.
  const other=candidates.find(x=>x.w.id!==c.w.id&&x.score-c.score<10&&x.pr.d<=p.accuracy);
  if(other)return {message:'Vias próximas: sentido ainda não confirmado'};
  if(roadName&&norm(c.w.tags.name)!==norm(roadName)&&norm(c.w.tags.ref)!==norm(roadName))return {message:'Confirmando a via atual…'};
  let w=c.w,index=c.sign===1?c.i+1:c.i,sign=c.sign,travel=distance(w.geometry[c.i],w.geometry[c.i+1])*(sign===1?1-c.pr.t:c.pr.t),incoming=heading;
  const current=norm(w.tags.name||w.tags.ref),seen=new Set();let first=null;
  for(let step=0;step<120&&travel<=1200;step++){
   const node=w.nodes[index],pt=w.geometry[index],key=w.id+':'+index+':'+sign;if(seen.has(key))break;seen.add(key);
   const crossings=(g.adjacent.get(node)||[]).filter(x=>x.id!==w.id&&norm(x.tags.name||x.tags.ref)!==current&&(x.tags.name||x.tags.ref));
   if(travel>=25&&crossings.length){
    const major=crossings.find(x=>/^(motorway|trunk|primary|secondary|tertiary)(_link)?$/.test(x.tags.highway));
    const cross=major||crossings[0],name=cross.tags.name||cross.tags.ref;
    const result={target:name,meters:Math.round(travel/10)*10,major:!!major,road:w.tags.name||w.tags.ref,message:'Em direção a '+name};
    if(major)return result;if(!first)first=result;
   }
   const next=index+sign;
   if(next>=0&&next<w.nodes.length){incoming=bearing(pt,w.geometry[next]);travel+=distance(pt,w.geometry[next]);index=next;continue;}
   const continuations=[];
   for(const nw of g.adjacent.get(node)||[]){if(nw.id===w.id||norm(nw.tags.name||nw.tags.ref)!==current)continue;for(let ni=0;ni<nw.nodes.length;ni++)if(nw.nodes[ni]===node)for(const ns of [1,-1]){const dest=ni+ns;if(dest<0||dest>=nw.nodes.length||!allowed(nw,ns))continue;const turn=delta(incoming,bearing(pt,nw.geometry[dest]));if(turn<40)continuations.push({nw,ni,ns,turn});}}
   if(continuations.length!==1)break;
   const q=continuations[0];w=q.nw;sign=q.ns;index=q.ni+sign;travel+=distance(pt,w.geometry[index]);incoming=bearing(pt,w.geometry[index]);
  }
  return first||{message:'Próxima conexão não identificada neste trecho'};
 }
 return {movement,graph,infer,distance,bearing,delta};
})();
if(typeof module!=='undefined')module.exports=RoadAhead;
