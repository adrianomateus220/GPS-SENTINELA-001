'use strict';
const HospitalSearch=(()=>{
 const norm=s=>String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
 const excluded=h=>/\b(upa|ubs|ama|caps)\b|unidade basica|pronto atendimento|hospital dia|maternidade|psiquiatr|oftalmolog|oncolog/.test(norm(h.name));
 function valid(h){return Number.isFinite(h.lat)&&Number.isFinite(h.lon)&&Math.abs(h.lat)<=90&&Math.abs(h.lon)<=180;}
 function discovered(elements){const out=[];for(const e of elements){const t=e.tags||{},pt=e.center||e,h={id:'osm-'+e.type+'-'+e.id,name:t.name||t['official_name'],lat:pt.lat,lon:pt.lon,city:t['addr:city']||'Município não informado',address:[t['addr:street'],t['addr:housenumber']].filter(Boolean).join(', ')||'Entrada de urgência a confirmar',source:'https://www.openstreetmap.org/'+e.type+'/'+e.id,services:'Capacidade para trauma não verificada',neuroEvidence:false,discovered:true};if(!h.name||!valid(h)||excluded(h)||t.emergency==='no'||!['hospital'].includes(t.amenity||t.healthcare))continue;if(out.some(x=>norm(x.name)===norm(h.name)&&Emergency.distance([x.lat,x.lon],[h.lat,h.lon])<0.5))continue;out.push(h);}return out;}
 function merge(known,found){const out=known.map(h=>({...h}));for(const h of found){if(out.some(x=>valid(x)&&Emergency.distance([x.lat,x.lon],[h.lat,h.lon])<0.7&&(norm(x.name)===norm(h.name)||x.mapSource===h.source)))continue;out.push(h);}return out;}
 function candidates(rows,p,radius){return Emergency.ranked(rows,p).filter(h=>valid(h)&&h.km<=radius&&!excluded(h));}
 function tableUrl(p,rows){return 'https://router.project-osrm.org/table/v1/driving/'+[[p.coords.longitude,p.coords.latitude],...rows.map(h=>[h.lon,h.lat])].map(x=>x.join(',')).join(';')+'?sources=0&annotations=distance,duration';}
 function metrics(data,rows){if(data.code!=='Ok'||!Array.isArray(data.distances?.[0])||data.distances[0].length!==rows.length+1)throw Error('Resposta de rotas inválida');return Object.fromEntries(rows.map((h,i)=>{const m=data.distances[0][i+1],t=data.durations?.[0]?.[i+1],snap=data.destinations?.[i+1]?.distance,originSnap=data.sources?.[0]?.distance;return [h.id,Number.isFinite(m)&&m>=0&&Number.isFinite(t)&&t>=0&&Number.isFinite(snap)&&snap<=250&&Number.isFinite(originSnap)&&originSnap<=100?{km:m/1000,minutes:t/60}:null];}));}
 function usable(origin,p,at,now=Date.now()){return Emergency.fresh(p,now)&&origin&&now-at<60000&&Emergency.distance(origin,[p.coords.latitude,p.coords.longitude])<0.1;}
 function ordered(rows,routes){return [...rows].sort((a,b)=>(routes[a.id]?.km??Infinity)-(routes[b.id]?.km??Infinity)||(a.km??Infinity)-(b.km??Infinity));}
 return {discovered,merge,candidates,tableUrl,metrics,usable,ordered,valid};
})();
if(typeof module!=='undefined')module.exports=HospitalSearch;
