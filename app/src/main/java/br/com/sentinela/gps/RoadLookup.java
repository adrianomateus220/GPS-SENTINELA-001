package br.com.sentinela.gps;
import java.io.File;
import java.util.*;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.datastore.*;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
/** Reads the actual saved geometry, never a last-known online address. */
public final class RoadLookup implements AutoCloseable {
 private final MapFile file;
 public RoadLookup(File f){file=new MapFile(f);}
 public static final class Result {public String road="Rua não identificada",number="Não cadastrado",district="Não cadastrado";public boolean matched;}
 static String tag(List<Tag> tags,String key){for(Tag t:tags)if(t.key.equals(key))return t.value;return "";}
 public Result at(double lat,double lon,double accuracy){
  Result r=new Result();if(accuracy>80)return r;
  byte z=16;long x=MercatorProjection.longitudeToTileX(lon,z),y=MercatorProjection.latitudeToTileY(lat,z);
  MapReadResult data=file.readMapData(new Tile(Math.max(0,x-1),Math.max(0,y-1),z,256),new Tile(Math.min(65535,x+1),Math.min(65535,y+1),z,256));
  if(data==null)return r;
  double best=Double.MAX_VALUE,second=Double.MAX_VALUE;Way chosen=null;
  for(Way w:data.ways){String name=tag(w.tags,"name");if(name.isEmpty()||tag(w.tags,"highway").isEmpty())continue;double d=wayDistance(w,lat,lon);if(d<best){if(chosen!=null&&!tag(chosen.tags,"name").equals(name))second=best;best=d;chosen=w;}else if(chosen!=null&&!name.equals(tag(chosen.tags,"name")))second=Math.min(second,d);}
  if(chosen==null||best>Math.min(45,Math.max(18,accuracy))||second-best<5){r.road=second-best<5?"Cruzamento — via ambígua":"Rua não identificada";return r;}
  r.road=tag(chosen.tags,"name");r.matched=true;
  String district=tag(chosen.tags,"addr:suburb");if(!district.isEmpty())r.district=district;
  double numberDistance=26;
  for(Way w:data.ways){String n=tag(w.tags,"addr:housenumber"),street=tag(w.tags,"addr:street");double d=wayDistance(w,lat,lon);if(!n.isEmpty()&&street.equals(r.road)&&d<numberDistance&&accuracy<=30){numberDistance=d;r.number=n+" (próximo)";}}
  for(PointOfInterest poi:data.pois){double d=meters(lat,lon,poi.position.latitude,poi.position.longitude);String n=tag(poi.tags,"addr:housenumber");if(!n.isEmpty()&&tag(poi.tags,"addr:street").equals(r.road)&&d<numberDistance&&accuracy<=30){numberDistance=d;r.number=n+" (próximo)";}}
  return r;
 }
 public static double meters(double lat,double lon,double a,double b){double dx=(b-lon)*111320*Math.cos(Math.toRadians(lat)),dy=(a-lat)*111320;return Math.hypot(dx,dy);}
 static double segment(double lat,double lon,LatLong a,LatLong b){double scale=111320*Math.cos(Math.toRadians(lat)),ax=(a.longitude-lon)*scale,ay=(a.latitude-lat)*111320,bx=(b.longitude-lon)*scale,by=(b.latitude-lat)*111320,dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy,t=den==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den));return Math.hypot(ax+t*dx,ay+t*dy);}
 static double wayDistance(Way w,double lat,double lon){double d=Double.MAX_VALUE;for(LatLong[] ring:w.latLongs){for(int i=1;i<ring.length;i++)d=Math.min(d,segment(lat,lon,ring[i-1],ring[i]));}return d;}
 public void close(){file.close();}
}
