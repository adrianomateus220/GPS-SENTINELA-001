package br.com.sentinela.gps;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Polygon containment, including holes. No network or Android services. */
public final class GeoAreas {
 private final List<JSONObject> features=new ArrayList<>();
 public GeoAreas(InputStream in)throws Exception{try(InputStream stream=in){JSONObject root=new JSONObject(new String(readBytes(stream),StandardCharsets.UTF_8));JSONArray fs=root.getJSONArray("features");for(int i=0;i<fs.length();i++)features.add(fs.getJSONObject(i));}}
 private static byte[] readBytes(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
 public JSONObject at(double lat,double lon){for(JSONObject f:features)try{if(contains(f.getJSONObject("geometry"),lat,lon))return f.getJSONObject("properties");}catch(JSONException ignored){}return null;}
 public static boolean contains(JSONObject g,double lat,double lon)throws JSONException{JSONArray c=g.getJSONArray("coordinates");if(g.getString("type").equals("Polygon"))return polygon(c,lat,lon);if(g.getString("type").equals("MultiPolygon")){for(int i=0;i<c.length();i++)if(polygon(c.getJSONArray(i),lat,lon))return true;}return false;}
 private static boolean polygon(JSONArray p,double y,double x)throws JSONException{if(p.length()==0||!ring(p.getJSONArray(0),y,x))return false;for(int i=1;i<p.length();i++)if(ring(p.getJSONArray(i),y,x))return false;return true;}
 private static boolean ring(JSONArray r,double y,double x)throws JSONException{boolean inside=false;for(int i=0,j=r.length()-1;i<r.length();j=i++){JSONArray a=r.getJSONArray(i),b=r.getJSONArray(j);double ax=a.getDouble(0),ay=a.getDouble(1),bx=b.getDouble(0),by=b.getDouble(1);if((ay>y)!=(by>y)&&x<(bx-ax)*(y-ay)/(by-ay)+ax)inside=!inside;}return inside;}
 public static String region(String uf){if("SP RJ MG ES".contains(uf)&&uf.length()==2)return "sudeste";if("PR SC RS".contains(uf)&&uf.length()==2)return "sul";if("DF GO MT MS".contains(uf)&&uf.length()==2)return "centro-oeste";if("AC AM AP PA RO RR TO".contains(uf)&&uf.length()==2)return "norte";if("AL BA CE MA PB PE PI RN SE".contains(uf)&&uf.length()==2)return "nordeste";throw new IllegalArgumentException("Estado inválido");}
 public static final String[] UFS={"AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"};
}
