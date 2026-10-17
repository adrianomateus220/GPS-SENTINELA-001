'use strict';
fetch('areas.geojson').then(r=>{if(!r.ok)throw Error();return r.json();}).then(data=>{
 const root=$('areaLayers');root.replaceChildren();
 if(!map||!window.L){root.textContent='Limites indisponíveis enquanto o mapa não carregar.';return;}
 const colors=['#ffbf47','#51c5df','#df8cfa','#75d190','#ff8f89'];
 [...new Set(data.features.map(f=>f.properties.layer))].forEach((name,i)=>{
  const features=data.features.filter(f=>f.properties.layer===name);
  const layer=L.geoJSON(features,{style:{color:colors[i%colors.length],weight:2,fillOpacity:0.06},onEachFeature:(f,l)=>{
   const content=document.createElement('div');content.style.color='#111';content.textContent=f.properties.name+' • '+name+' • referência JAN23, vigência não confirmada';l.bindPopup(content);
  }}).addTo(map);
  const label=el('label','', 'check'),check=document.createElement('input');check.type='checkbox';check.checked=true;check.onchange=()=>check.checked?layer.addTo(map):map.removeLayer(layer);label.append(check,document.createTextNode(' '+name+' ('+features.length+' áreas)'));root.append(label);
 });
}).catch(()=>{$('areaLayers').textContent='Não foi possível carregar os limites.';});
