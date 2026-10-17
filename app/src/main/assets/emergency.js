'use strict';
const Emergency = (() => {
 function distance(a,b){const r=Math.PI/180,dlat=(b[0]-a[0])*r,dlon=(b[1]-a[1])*r;const x=Math.sin(dlat/2)**2+Math.cos(a[0]*r)*Math.cos(b[0]*r)*Math.sin(dlon/2)**2;return 6371*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x));}
 function fresh(p,now=Date.now()){return !!p&&now-p.timestamp<=30000&&now>=p.timestamp&&Number.isFinite(p.coords.accuracy)&&p.coords.accuracy<=100;}
 function ranked(rows,p){return rows.map(h=>({...h,km:p&&Number.isFinite(h.lat)?distance([p.coords.latitude,p.coords.longitude],[h.lat,h.lon]):null})).sort((a,b)=>(a.km??Infinity)-(b.km??Infinity)||a.name.localeCompare(b.name));}
 function route(destination){return 'https://www.google.com/maps/dir/?api=1&destination='+encodeURIComponent(destination)+'&travelmode=driving&dir_action=navigate';}
 return {distance,fresh,ranked,route};
})();
if(typeof module!=='undefined')module.exports=Emergency;
