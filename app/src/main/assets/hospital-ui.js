'use strict';
window.HospitalUI=(()=>{
 let found=[],routes={},origin=null,at=0,busy=false,requestAt=0,radius=50,request=0,loaded=false;
 let message='Toque em emergência para buscar pela posição atual.';
 const confirmed=new Set();
 function routeValid(){return HospitalSearch.usable(origin,lastFix,at);}
 function distanceText(h,valid){const r=valid?routes[h.id]:null;return r?`${r.km.toFixed(1)} km pelo trajeto calculado • ~${Math.ceil(r.minutes)} min sem trânsito`:Emergency.fresh(lastFix)&&Number.isFinite(h.km)?`${h.km.toFixed(1)} km em linha reta • trajeto não confirmado`:'Distância indisponível • atualize a localização';}
 function openRoute(h){if(confirm(`${h.name}\n\nA regulação confirmou que este hospital recebe este caso agora e informou a entrada de urgência?`))location.href=Emergency.route(`${h.lat},${h.lon}`);}
 function card(h,valid,index,complete){const c=el('article','','hospital');c.dataset.id=h.id;if(index===0&&complete)c.append(el('p','MENOR DISTÂNCIA ENTRE OS TRAJETOS DESTA LISTA','distance'));c.append(el('h3',h.name),el('p',h.city+' • '+(h.discovered&&!confirmed.has(h.id)?'CAPACIDADE A CONFIRMAR':'perfil documentado / confirmação operacional necessária'),'sub'),el('p',h.services),el('p',distanceText(h,valid),'distance'),el('p',h.address,'sub'));
 const source=el('a',h.discovered?'Consultar cadastro no mapa':'Consultar fonte institucional');source.href=h.source;source.className='source';c.append(source);
 if(h.discovered&&!confirmed.has(h.id)){const b=el('button','Regulação confirmou este hospital');b.className='alt';b.onclick=()=>{if(confirm('A regulação confirmou atendimento ao trauma deste caso, recebimento agora e acesso ao hospital? Esta confirmação vale apenas para esta sessão.')){confirmed.add(h.id);render();}};c.append(b);}else{const b=el('button','Confirmar destino e abrir GPS');b.onclick=()=>openRoute(h);c.append(b);}return c;}
 function render(){const root=$('hospitals'),other=$('otherHospitals');root.replaceChildren();other.replaceChildren();const fresh=Emergency.fresh(lastFix),valid=routeValid(),term=$('filterHospital').value.toLocaleLowerCase('pt-BR'),neuro=$('filterNeuro').checked;
 let rows=fresh?HospitalSearch.candidates(HospitalSearch.merge(hospitals,found),lastFix,radius):[];
 rows=HospitalSearch.ordered(rows,valid?routes:{}).filter(h=>(!neuro||h.neuroEvidence)&&[h.name,h.city].join(' ').toLocaleLowerCase('pt-BR').includes(term));const eligible=rows.filter(h=>!h.discovered||confirmed.has(h.id));const unknown=rows.filter(h=>h.discovered&&!confirmed.has(h.id));const complete=valid&&eligible.length>0&&eligible.every(h=>routes[h.id]);
 $('coverage').textContent=`${hospitals.length} referências institucionais + busca no mapa ao redor de você (raio de ${radius} km). Cobertura não exaustiva. Não confirma vagas, plantão nem atendimento ao caso.`;
 $('nearbyStatus').textContent=!fresh?'Aguardando localização recente para buscar hospitais próximos.':message+(valid?'': ' Sem comparação atual de trajetos.');
 if(!eligible.length)root.append(el('p',fresh?'Nenhuma referência documentada neste raio/filtro. Amplie a busca ou use o destino indicado pela regulação.':'Obtenha a localização para consultar hospitais próximos.'));
 eligible.forEach((h,i)=>root.append(card(h,valid,i,complete)));unknown.forEach(h=>other.append(card(h,valid,-1,false)));if(!unknown.length)other.append(el('p','Nenhum outro hospital encontrado nesta busca.'));
 }
 async function search(force=false){
  if(!loaded||busy||!Emergency.fresh(lastFix))return render();
  if(!force&&Date.now()-requestAt<20000)return;
  busy=true;requestAt=Date.now();const token=++request,p={timestamp:lastFix.timestamp,coords:{latitude:lastFix.coords.latitude,longitude:lastFix.coords.longitude,accuracy:lastFix.coords.accuracy}},point=[p.coords.latitude,p.coords.longitude];routes={};origin=null;at=0;found=[];message='Buscando hospitais próximos…';render();
  let discoveryOK=false,routeOK=true;
  try{const q=`[out:json][timeout:15];(nwr(around:${radius*1000},${point[0]},${point[1]})[amenity=hospital];nwr(around:${radius*1000},${point[0]},${point[1]})[healthcare=hospital];);out center tags;`;const res=await fetch('https://overpass-api.de/api/interpreter',{method:'POST',body:new URLSearchParams({data:q}),signal:AbortSignal.timeout(20000)});if(!res.ok)throw Error();const data=await res.json();if(data.remark||!Array.isArray(data.elements))throw Error();found=HospitalSearch.discovered(data.elements);discoveryOK=true;}catch(e){message='Busca online indisponível; consultando apenas referências cadastradas.';}
  const rows=HospitalSearch.candidates(HospitalSearch.merge(hospitals,found),p,radius);message=discoveryOK?'Comparando trajetos dos hospitais encontrados…':message;render();const computed={};
  for(let i=0;i<rows.length;i+=40){if(token!==request)break;const batch=rows.slice(i,i+40);try{const res=await fetch(HospitalSearch.tableUrl(p,batch),{signal:AbortSignal.timeout(15000)});if(!res.ok)throw Error();Object.assign(computed,HospitalSearch.metrics(await res.json(),batch));}catch(e){routeOK=false;}}
  if(token===request){if(Emergency.fresh(lastFix)&&Emergency.distance(point,[lastFix.coords.latitude,lastFix.coords.longitude])<0.1){routes=computed;origin=point;at=Date.now();message=(discoveryOK?'Busca online concluída. ':'Busca online falhou; somente catálogo. ')+(routeOK?'Menor distância entre os trajetos calculados primeiro. Não garante o hospital mais próximo fora desta busca.':'Cálculo de trajetos incompleto; distâncias em linha reta não são trajetos.');}else{routes={};origin=null;message='Posição mudou durante a busca. Atualize para comparar os trajetos.';}busy=false;render();}
 }
 function onFix(){if(!$('emergencyPanel').hidden&&!routeValid())search();}
 $('emergency').onclick=()=>{$('emergencyPanel').hidden=false;$('emergencyPanel').scrollIntoView({behavior:'smooth'});tick();render();once();search();};
 $('refreshHospitals').onclick=()=>{once();search(true);};$('expandHospitals').onclick=()=>{if(busy)return;radius=radius===50?100:50;$('expandHospitals').textContent=radius===50?'Ampliar busca para 100 km':'Reduzir busca para 50 km';search(true);};
 fetch('hospitals.json').then(r=>{if(!r.ok)throw Error();return r.json();}).then(d=>{hospitals=d;loaded=true;render();onFix();}).catch(()=>{loaded=true;message='Catálogo indisponível; a busca online exige confirmação de capacidade.';render();onFix();});
 setInterval(()=>{if(!$('emergencyPanel').hidden){render();onFix();}},5000);
 return {render,onFix,search};
})();
