package br.com.sentinela.gps;
import java.util.*;
/** Immutable road geometry; no location history is stored here. */
public final class RoadIndex {
 public static final class Segment {
  public final String name,district;public final double a,b,c,d;
  public Segment(String n,String sub,double lat1,double lon1,double lat2,double lon2){name=n;district=sub;a=lat1;b=lon1;c=lat2;d=lon2;}
 }
 public static final class Match {public String name="Rua não identificada",district="Não cadastrado";public boolean matched;public int examined;}
 public static Match match(Collection<Segment> segments,double lat,double lon,double accuracy,Double heading,String previous){
  Match out=new Match();if(!Double.isFinite(lat)||!Double.isFinite(lon)||!Double.isFinite(accuracy)||accuracy<0||accuracy>80)return out;
  double scale=111320*Math.max(.01,Math.cos(Math.toRadians(lat))),limit=Math.min(45,Math.max(18,accuracy));
  Map<String,double[]> scores=new HashMap<>();Map<String,String> districts=new HashMap<>();
  for(Segment s:segments){
   if(lat<Math.min(s.a,s.c)-limit/111320||lat>Math.max(s.a,s.c)+limit/111320||lon<Math.min(s.b,s.d)-limit/scale||lon>Math.max(s.b,s.d)+limit/scale)continue;
   out.examined++;double ax=(s.b-lon)*scale,ay=(s.a-lat)*111320,dx=(s.d-s.b)*scale,dy=(s.c-s.a)*111320,den=dx*dx+dy*dy,t=den==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den)),dist=Math.hypot(ax+t*dx,ay+t*dy);if(dist>limit)continue;
   double penalty=0;if(heading!=null&&den>16){double bearing=(Math.toDegrees(Math.atan2(dx,dy))+360)%360,diff=Math.abs(bearing-heading)%180;diff=Math.min(diff,180-diff);penalty=12*diff/90;}
   double score=dist+penalty-(s.name.equals(previous)?2:0);double[] old=scores.get(s.name);if(old==null||score<old[0]){scores.put(s.name,new double[]{score,dist});districts.put(s.name,s.district);}
  }
  String best=null;double first=Double.MAX_VALUE,second=Double.MAX_VALUE;
  for(Map.Entry<String,double[]> e:scores.entrySet()){double score=e.getValue()[0];if(score<first){second=first;first=score;best=e.getKey();}else second=Math.min(second,score);}
  if(best==null)return out;if(second-first<3){out.name="Cruzamento — via ambígua";return out;}
  out.name=best;out.matched=true;String sub=districts.get(best);if(sub!=null&&!sub.isEmpty())out.district=sub;return out;
 }
}
