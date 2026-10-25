'use strict';
let areasLoaded=false,areasLoading=false;
async function loadAreas(){
 if(areasLoaded||areasLoading||!$('areasToggle').open)return;
 const root=$('areaLayers');
 if(!map||!window.L){root.textContent='Aguardando o mapa…';return;}
 areasLoading=true;root.textContent='Carregando limites…';
 try{
  const r=await fetch('areas.geojson');if(!r.ok)throw Error();const data=await r.json();root.replaceChildren();
  const colors=['#ffbf47','#51c5df','#df8cfa','#75d190','#ff8f89'];
  [...new Set(data.features.map(f=>f.properties.layer))].forEach((name,i)=>{
   const features=data.features.filter(f=>f.properties.layer===name);
   const layer=L.geoJSON(features,{style:{color:colors[i%colors.length],weight:2,fillOpacity:0.06},onEachFeature:(f,l)=>{const content=document.createElement('div');content.style.color='#111';content.textContent=f.properties.name+' • '+name+' • referência JAN23, vigência não confirmada';l.bindPopup(content);}}).addTo(map);
   const label=document.createElement('label'),check=document.createElement('input');label.className='check';check.type='checkbox';check.checked=true;check.onchange=()=>check.checked?layer.addTo(map):map.removeLayer(layer);label.append(check,document.createTextNode(' '+name+' ('+features.length+' áreas)'));root.append(label);
  });areasLoaded=true;
 }catch(e){root.textContent='Não foi possível carregar os limites. Feche e abra para tentar novamente.';}finally{areasLoading=false;}
}
$('areasToggle').addEventListener('toggle',loadAreas);
window.addEventListener('sentinela-map-ready',loadAreas);
