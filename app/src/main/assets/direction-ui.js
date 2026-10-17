'use strict';
let roadGraph=null,graphCenter=null,graphAt=0,graphBusy=false,graphRequestAt=0,movementHistory=[],aheadMessage='Aguardando deslocamento',aheadTarget=null,aheadFixAt=0,aheadRequestId=0;
function clearAhead(message){aheadMessage=message;aheadTarget=null;$('direction').textContent=message;$('directionDetail').textContent='Estimativa pela malha viária; confirme visualmente.';}
function renderAhead(){
 const p=lastFix;if(!p||Date.now()-p.timestamp>8000||p.coords.accuracy>35){clearAhead('Aguardando deslocamento e GPS preciso');return;}
 const heading=RoadAhead.movement(movementHistory);if(heading===null){clearAhead('Aguardando deslocamento confirmado');return;}
 if(!roadGraph||!graphCenter||Date.now()-graphAt>180000||RoadAhead.distance({lat:p.coords.latitude,lon:p.coords.longitude},graphCenter)>550){clearAhead('Consultando vias à frente…');return;}
 const result=RoadAhead.infer(roadGraph,{lat:p.coords.latitude,lon:p.coords.longitude,accuracy:p.coords.accuracy},heading,addressValid()?lastRoad:null);
 aheadMessage=result.message;aheadTarget=result.target||null;aheadFixAt=p.timestamp;$('direction').textContent=result.message;
 $('directionDetail').textContent=result.target?`${result.major?'Conexão com via principal':'Próximo cruzamento cadastrado'} a cerca de ${result.meters} m, seguindo a via. Estimativa; não prevê conversões.`:'Estimativa pela malha viária; confirme visualmente.';
}
function trackAhead(p){
 movementHistory.push({lat:p.coords.latitude,lon:p.coords.longitude,accuracy:p.coords.accuracy,heading:p.coords.heading,time:p.timestamp});movementHistory=movementHistory.filter(x=>p.timestamp-x.time<=20000).slice(-30);renderAhead();
 const point={lat:p.coords.latitude,lon:p.coords.longitude};
 if(p.coords.accuracy>35||graphBusy||Date.now()-graphRequestAt<30000)return;
 if(graphCenter&&RoadAhead.distance(point,graphCenter)<350&&Date.now()-graphAt<180000)return;
 loadRoadGraph(point);
}
async function loadRoadGraph(point){
 graphBusy=true;graphRequestAt=Date.now();const requestId=++aheadRequestId;
 try{
  const query=`[out:json][timeout:12];way(around:1300,${point.lat},${point.lon})[highway~"^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|living_street|service)(_link)?$"];out body geom;`;
  const res=await fetch('https://overpass-api.de/api/interpreter',{method:'POST',body:new URLSearchParams({data:query}),signal:AbortSignal.timeout(18000)});
  if(!res.ok)throw Error();const d=await res.json();if(requestId!==aheadRequestId)return;
  if(d.remark||!Array.isArray(d.elements)||!d.elements.length)throw Error();roadGraph=RoadAhead.graph(d.elements);graphCenter=point;graphAt=Date.now();renderAhead();
 }catch(e){clearAhead('Consulta de vias indisponível');}finally{graphBusy=false;}
}
