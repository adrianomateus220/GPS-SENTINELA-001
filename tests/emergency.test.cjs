const assert=require('node:assert/strict');
const E=require('../app/src/main/assets/emergency.js');
const fix={timestamp:100000,coords:{latitude:-23.55,longitude:-46.63,accuracy:20}};
assert(E.fresh(fix,100010));assert(!E.fresh(fix,140000));assert(!E.fresh({...fix,coords:{...fix.coords,accuracy:150}},100010));assert(!E.fresh(null));
assert(Math.abs(E.distance([0,0],[0,1])-111.195)<0.1);
const hospitals=[{name:'Longe',lat:-20,lon:-49},{name:'Perto',lat:-23.56,lon:-46.64}];
assert.equal(E.ranked(hospitals,fix)[0].name,'Perto');assert.equal(E.ranked(hospitals,null)[0].km,null);
const u=new URL(E.route('Hospital A & B, São Paulo'));assert.equal(u.searchParams.get('destination'),'Hospital A & B, São Paulo');assert.equal(u.searchParams.get('dir_action'),'navigate');
console.log('GPS antigo, precisão, distância e destino: OK');
