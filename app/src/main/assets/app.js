'use strict';
const $=id=>document.getElementById(id);
const GEO_INTERVAL=1500, ADDRESS_MAX_AGE=20000, ADDRESS_MAX_DISTANCE=30;
let map=null,marker=null,circle=null,watch=null,voice=false,lastFix=null,lastRoad='';
let address='',addressPos=null,addressAt=0,lastGeoTime=0,geoBusy=false,track=false;
let geoController=null,generation=0,retryDelay=GEO_INTERVAL;
let travelHeading=null,headingAnchor=null;
function updateHeading(p){
 const c=p.coords,pos=[c.latitude,c.longitude];
 if(Number.isFinite(c.heading)&&Number.isFinite(c.speed)&&c.speed>0.8){travelHeading=c.heading;headingAnchor=p;return;}
 if(!headingAnchor){headingAnchor=p;return;}
 const a=headingAnchor.coords,from=[a.latitude,a.longitude],moved=distance(from,pos);
 if(c.accuracy<=50&&a.accuracy<=50&&moved>=Math.max(8,(c.accuracy+a.accuracy)/2)){
  const r=Math.PI/180,dl=(c.longitude-a.longitude)*r,y=Math.sin(dl)*Math.cos(c.latitude*r),x=Math.cos(a.latitude*r)*Math.sin(c.latitude*r)-Math.sin(a.latitude*r)*Math.cos(c.latitude*r)*Math.cos(dl);
  travelHeading=(Math.atan2(y,x)/r+360)%360;headingAnchor=p;
 }else if(p.timestamp-headingAnchor.timestamp>15000)headingAnchor=p;
}
function distance(a,b){const r=Math.PI/180,dlat=(b[0]-a[0])*r,dlon=(b[1]-a[1])*r;const h=Math.sin(dlat/2)**2+Math.cos(a[0]*r)*Math.cos(b[0]*r)*Math.sin(dlon/2)**2;return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));}
function point(){return lastFix?[lastFix.coords.latitude,lastFix.coords.longitude]:null;}
function fresh(){return lastFix&&Date.now()-lastFix.timestamp<15000;}
function addressValid(){return fresh()&&addressPos&&Date.now()-addressAt<ADDRESS_MAX_AGE&&distance(addressPos,point())<ADDRESS_MAX_DISTANCE;}
function speak(t){if(!voice||!('speechSynthesis'in window))return;speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(t);u.lang='pt-BR';speechSynthesis.speak(u);}
function clearAddress(){$('houseNumber').title='';address='';addressPos=null;addressAt=0;$('road').textContent='Atualizando rua…';$('houseNumber').textContent='Não disponível';$('district').textContent='—';$('municipality').textContent='—';$('postal').textContent='—';$('place').textContent='Aguardando endereço da posição atual';}
function locationText(){if(!lastFix)return 'Posição ainda não obtida.';const p=lastFix.coords;return ['GPS Sentinela — localização',addressValid()?address:'Endereço não disponível para esta posição',`Coordenadas: ${p.latitude.toFixed(6)}, ${p.longitude.toFixed(6)}`,`Posição de ${new Date(lastFix.timestamp).toLocaleString('pt-BR')}`,`Referência: ${$('reference').value.trim()||'não informada'}`,`Mapa: https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}`].join('\n');}
async function copyLocation(){if(!lastFix){$('status').textContent='Aguardando localização para copiar.';return;}try{await navigator.clipboard.writeText(locationText());$('status').textContent='Localização copiada';}catch(e){$('copytext').value=locationText();$('copybox').hidden=false;$('copytext').select();}}
let nativeNumberBusy=false;
function normalizeRoad(name){return String(name||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/\b(r|av|avenida|rua)\.?\s+/g,'').replace(/[^a-z0-9]/g,'');}
async function enrichNumber(pos,epoch,road,stamp){
 if(nativeNumberBusy||location.hostname!=='appassets.androidplatform.net')return;
 nativeNumberBusy=true;const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),3500);
 try{
  const r=await fetch(`/native-address?lat=${pos[0]}&lon=${pos[1]}`,{signal:controller.signal});if(!r.ok)return;
  const a=await r.json(),number=String(a.house_number||'').trim();
  // A nearby cross street, an old fix or a distant result must never supply this number.
  if(!number||!/[0-9]/.test(number)||normalizeRoad(a.road)!==normalizeRoad(road))return;
  if(!Number.isFinite(a.lat)||!Number.isFinite(a.lon)||distance(pos,[a.lat,a.lon])>25)return;
  if(epoch!==generation||!track||stamp!==addressAt||!addressValid()||distance(pos,point())>15||lastFix.coords.accuracy>50)return;
  $('houseNumber').textContent=number;$('houseNumber').title='Número informado pelo cadastro de endereços do Android; confirme o imóvel no local.';
  const district=$('district').textContent;
  $('place').textContent=['nº '+number,district].join(' • ');
  address=[road,'nº '+number,district,$('municipality').textContent].join(' • ');
 }catch(e){}finally{clearTimeout(timer);nativeNumberBusy=false;}
}
async function geocode(){
 if(!track||geoBusy||!fresh()||Date.now()-lastGeoTime<retryDelay)return;
 const pos=point();
 if(addressValid()&&distance(addressPos,pos)<8&&Date.now()-addressAt<15000)return;
 geoBusy=true;lastGeoTime=Date.now();const epoch=generation,controller=new AbortController();geoController=controller;
 const timeout=setTimeout(()=>controller.abort(),4000);
 try{
  const res=await fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${pos[0]}&lon=${pos[1]}&zoom=18&addressdetails=1&accept-language=pt-BR`,{signal:controller.signal});
  if(!res.ok)throw Error('geocode');const j=await res.json();
  if(epoch!==generation||!track)return;
  retryDelay=GEO_INTERVAL;
  // Never let an old response overwrite the address of the current position.
  if(!fresh()||distance(pos,point())>=ADDRESS_MAX_DISTANCE)return;
  const a=j.address||{},road=a.road||a.pedestrian||a.highway;
  if(!road)throw Error('no street');
  const number=a.house_number||'Não disponível',district=a.suburb||a.neighbourhood||a.city_district||'Não disponível';
  address=[road,a.house_number?'nº '+a.house_number:'Número não disponível',district,a.city||a.town||a.municipality||a.village].filter(Boolean).join(' • ');
  addressPos=pos;addressAt=Date.now();$('road').textContent=road;$('houseNumber').textContent=number;$('district').textContent=district;$('place').textContent=[a.house_number?'nº '+a.house_number:'Número não disponível',district].join(' • ');
  $('municipality').textContent=[a.city||a.town||a.municipality||a.village,a.state].filter(Boolean).join(' / ')||'Não disponível';$('postal').textContent=a.postcode||'Não disponível';
  if(!a.house_number)enrichNumber(pos,epoch,road,addressAt);
  if(road!==lastRoad){lastRoad=road;speak('Via atual: '+road);}
 }catch(e){if(epoch===generation&&track){retryDelay=Math.min(30000,retryDelay*2);if(!addressValid()){clearAddress();$('place').textContent='Endereço indisponível. Tentando novamente…';}}}
 finally{clearTimeout(timeout);if(epoch===generation){geoBusy=false;geoController=null;geocode();}}
}
function renderMap(){if(!map||!lastFix)return;const c=lastFix.coords,p=[c.latitude,c.longitude];if(!marker){marker=L.marker(p,{icon:L.divIcon({className:'',html:'<div class="position-arrow"><svg viewBox="0 0 40 40" aria-hidden="true"><path d="M20 3 L34 35 L20 28 L6 35 Z" fill="#b89b58" stroke="white" stroke-width="2.5" stroke-linejoin="round"/></svg></div>',iconSize:[40,40],iconAnchor:[20,20]})}).addTo(map);circle=L.circle(p,{radius:c.accuracy,color:'#b89b58',weight:1,fillOpacity:0.08}).addTo(map);map.setZoom(16);}else{marker.setLatLng(p);circle.setLatLng(p).setRadius(c.accuracy);}const arrow=marker.getElement()?.querySelector('.position-arrow');if(arrow){arrow.style.transform=`rotate(${travelHeading??0}deg)`;arrow.title=travelHeading===null?'Posição atual — direção ainda não identificada':'Direção do deslocamento';}map.panTo(p,{animate:false});}
function update(p){if(!track||Date.now()-p.timestamp>15000||(lastFix&&p.timestamp<lastFix.timestamp))return;updateHeading(p);lastFix=p;const c=p.coords;$('coords').textContent=c.latitude.toFixed(6)+', '+c.longitude.toFixed(6);$('status').textContent=c.accuracy>100?'GPS com precisão baixa':'Localização ativa';if(address&&!addressValid())clearAddress();renderMap();tick();geocode();}
function gpsError(e){$('status').textContent=e.code===1?'Permita a localização nas configurações do aplicativo.':e.code===3?'Aguardando sinal do GPS…':'Localização indisponível. Verifique o GPS.';if(e.code===1){pause();$('pause').textContent='Tentar localização';}}
function start(){if(track)return;if(!navigator.geolocation){$('status').textContent='GPS indisponível';return;}track=true;$('pause').textContent='Pausar localização';$('status').textContent='Obtendo localização…';watch=navigator.geolocation.watchPosition(update,gpsError,{enableHighAccuracy:true,maximumAge:0,timeout:10000});}
function pause(){track=false;generation++;if(watch!==null)navigator.geolocation.clearWatch(watch);watch=null;if(geoController)geoController.abort();geoController=null;geoBusy=false;$('pause').textContent='Retomar localização';}
function tick(){const recent=fresh();$('gpslive').textContent=!track?'○ PAUSADO':recent?'● LOCALIZAÇÃO ATIVA':'○ AGUARDANDO GPS';if(address&&!addressValid())clearAddress();if(track)geocode();}
$('pause').onclick=()=>{if(track){pause();$('status').textContent='Localização pausada';}else start();};
$('voice').onclick=()=>{voice=!voice;$('voice').textContent='Voz: '+(voice?'ligada':'desligada');};$('copy').onclick=copyLocation;
setInterval(tick,500);
document.addEventListener('visibilitychange',()=>{if(!document.hidden){if(track){pause();start();}tick();}});
window.addEventListener('pagehide',pause);
window.addEventListener('pageshow',e=>{if(e.persisted)start();});
// GPS starts before any remote map library or boundary dataset is loaded.
start();
function initMap(){if(map||!window.L)return;map=L.map('map').setView([-23.55,-46.63],12);// Display tiles at double size; zoomOffset preserves their geographic alignment and viewport.
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{tileSize:512,zoomOffset:-1,minZoom:1,maxZoom:20,maxNativeZoom:20,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'}).addTo(map);renderMap();window.dispatchEvent(new Event('sentinela-map-ready'));}
const css=document.createElement('link');css.rel='stylesheet';css.href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';document.head.append(css);
const leaflet=document.createElement('script');leaflet.src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';leaflet.async=true;leaflet.onload=initMap;leaflet.onerror=()=>{$('map').textContent='Mapa indisponível. A localização continua ativa.';};document.head.append(leaflet);
