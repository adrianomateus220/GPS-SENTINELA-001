package br.com.sentinela.gps;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.datastore.*;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
/** Bounded RAM/disk caches contain road geometry, never timestamped GPS traces. */
public final class RoadLookup implements AutoCloseable {
 private final MapFile file,aheadFile;private final Object foreground=new Object(),background=new Object();
 private final File disk;private volatile boolean closed;private final ExecutorService ahead=Executors.newSingleThreadExecutor();
 private final LatestTask<double[]> prefetch=new LatestTask<>(ahead,this::warm);
 private final Map<Long,List<RoadIndex.Segment>> cache=new LinkedHashMap<Long,List<RoadIndex.Segment>>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<Long,List<RoadIndex.Segment>> e){return size()>64;}};
 private final Map<Long,List<RoadIndex.Segment>> saves=new LinkedHashMap<>();
 public volatile long lastLookupMicros;public long mapReads,ramHits,diskHits;private String previous="";private double prevLat,prevLon;private long prevTime;
 public RoadLookup(File f){this(f,null);}
 public RoadLookup(File f,File cacheDir){file=new MapFile(f);aheadFile=new MapFile(f);disk=cacheDir==null?null:new File(cacheDir,"roads-v2-"+f.length()+"-"+f.lastModified());if(disk!=null)disk.mkdirs();}
 public static final class Result {public String road="Rua não identificada",number="Não cadastrado",district="Não cadastrado";public boolean matched;}
 static String tag(List<Tag> tags,String key){for(Tag t:tags)if(t.key.equals(key))return t.value;return "";}
 private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
 private List<RoadIndex.Segment> tile(int x,int y,boolean warming){
  if(closed||x<0||y<0||x>65535||y>65535)return Collections.emptyList();long key=key(x,y);
  synchronized(cache){List<RoadIndex.Segment> hit=cache.get(key);if(hit!=null){ramHits++;return hit;}}
  List<RoadIndex.Segment> loaded=readDisk(key);
  if(loaded==null){loaded=new ArrayList<>();MapReadResult data;Object lock=warming?background:foreground;
   synchronized(lock){if(closed)return Collections.emptyList();data=(warming?aheadFile:file).readMapData(new Tile(x,y,(byte)16,256));}
   synchronized(cache){mapReads++;}
   if(data!=null)for(Way w:data.ways){String name=tag(w.tags,"name");if(name.isEmpty()||tag(w.tags,"highway").isEmpty())continue;String sub=tag(w.tags,"addr:suburb");for(LatLong[] line:w.latLongs)for(int i=1;i<line.length;i++)loaded.add(new RoadIndex.Segment(name,sub,line[i-1].latitude,line[i-1].longitude,line[i].latitude,line[i].longitude));}
   if(disk!=null)synchronized(saves){if(saves.size()<64)saves.put(key,loaded);}
  }
  loaded=Collections.unmodifiableList(loaded);synchronized(cache){cache.put(key,loaded);int count=0;for(List<RoadIndex.Segment> roads:cache.values())count+=roads.size();Iterator<Map.Entry<Long,List<RoadIndex.Segment>>> it=cache.entrySet().iterator();while(count>150000&&cache.size()>1&&it.hasNext()){count-=it.next().getValue().size();it.remove();}}return loaded;
 }
 public Result at(double lat,double lon,double accuracy){return at(lat,lon,accuracy,null);}
 public Result at(double lat,double lon,double accuracy,Double heading){
  long start=System.nanoTime();Result r=new Result();try{
   if(closed||!Double.isFinite(lat)||!Double.isFinite(lon)||!Double.isFinite(accuracy)||Math.abs(lat)>85||Math.abs(lon)>180||accuracy<0||accuracy>80)return r;
   int x=MercatorProjection.longitudeToTileX(lon,(byte)16),y=MercatorProjection.latitudeToTileY(lat,(byte)16);List<RoadIndex.Segment> near=new ArrayList<>();
   // Query only tiles intersecting the matching radius, including boundary neighbours.
   double radius=Math.min(45,Math.max(18,accuracy)),dlat=radius/111320,dlon=radius/(111320*Math.max(.01,Math.cos(Math.toRadians(lat))));
   int minX=MercatorProjection.longitudeToTileX(lon-dlon,(byte)16),maxX=MercatorProjection.longitudeToTileX(lon+dlon,(byte)16),minY=MercatorProjection.latitudeToTileY(lat+dlat,(byte)16),maxY=MercatorProjection.latitudeToTileY(lat-dlat,(byte)16);
   for(int xx=minX;xx<=maxX;xx++)for(int yy=minY;yy<=maxY;yy++)near.addAll(tile(xx,yy,false));
   long now=System.nanoTime();String prior=now-prevTime<6_000_000_000L&&meters(lat,lon,prevLat,prevLon)<120?previous:"";
   RoadIndex.Match m=RoadIndex.match(near,lat,lon,accuracy,heading,prior);r.road=m.name;r.district=m.district;r.matched=m.matched;
   if(m.matched){previous=m.name;prevLat=lat;prevLon=lon;prevTime=now;}else previous="";
   return r;
  }finally{lastLookupMicros=(System.nanoTime()-start)/1000;}
 }
 public void prepare(double lat,double lon,double speed,Double bearing){if(!closed&&Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<85&&Math.abs(lon)<=180)prefetch.submit(new double[]{lat,lon,Math.max(0,speed),bearing==null?Double.NaN:bearing});}
 private void warm(double[] p){if(closed)return;int x=MercatorProjection.longitudeToTileX(p[1],(byte)16),y=MercatorProjection.latitudeToTileY(p[0],(byte)16);
  tile(x,y,true);if(Double.isFinite(p[3])){double distance=Math.min(900,Math.max(150,p[2]*12)),angle=Math.toRadians(p[3]);for(int i=1;i<=2&&!closed;i++){double lat=p[0]+Math.cos(angle)*distance*i/2/111320,lon=p[1]+Math.sin(angle)*distance*i/2/(111320*Math.cos(Math.toRadians(p[0])));tile(MercatorProjection.longitudeToTileX(lon,(byte)16),MercatorProjection.latitudeToTileY(lat,(byte)16),true);}}
  for(int dx=-1;dx<=1&&!closed;dx++)for(int dy=-1;dy<=1&&!closed;dy++)tile(x+dx,y+dy,true);flush();
 }
 private List<RoadIndex.Segment> readDisk(long key){if(disk==null)return null;File f=new File(disk,key+".bin");if(!f.isFile())return null;try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){if(in.readInt()!=0x53454e32)return null;int n=in.readInt();if(n<0||n>100000)return null;List<RoadIndex.Segment> roads=new ArrayList<>(n);for(int i=0;i<n;i++){String name=in.readUTF(),sub=in.readUTF();double a=in.readDouble(),b=in.readDouble(),c=in.readDouble(),d=in.readDouble();if(!Double.isFinite(a)||!Double.isFinite(b)||!Double.isFinite(c)||!Double.isFinite(d))return null;roads.add(new RoadIndex.Segment(name,sub,a,b,c,d));}synchronized(cache){diskHits++;}return roads;}catch(IOException e){return null;}}
 private void flush(){if(disk==null||closed)return;Map<Long,List<RoadIndex.Segment>> batch;synchronized(saves){batch=new LinkedHashMap<>(saves);saves.clear();}
  for(Map.Entry<Long,List<RoadIndex.Segment>> e:batch.entrySet()){if(closed)return;File tmp=new File(disk,e.getKey()+".tmp"),dest=new File(disk,e.getKey()+".bin");try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))){out.writeInt(0x53454e32);out.writeInt(e.getValue().size());for(RoadIndex.Segment s:e.getValue()){out.writeUTF(s.name);out.writeUTF(s.district);out.writeDouble(s.a);out.writeDouble(s.b);out.writeDouble(s.c);out.writeDouble(s.d);}}catch(IOException ex){tmp.delete();continue;}if(!tmp.renameTo(dest))tmp.delete();}
  File[] files=disk.listFiles((dir,name)->name.endsWith(".bin"));if(files!=null){Arrays.sort(files,Comparator.comparingLong(File::lastModified));long bytes=0;for(File f:files)bytes+=f.length();for(int i=0;i<files.length&&(files.length-i>256||bytes>32*1024*1024);i++){long size=files[i].length();if(files[i].delete())bytes-=size;}}
 }
 public void resetMatch(){previous="";prevTime=0;}
 public static double meters(double lat,double lon,double a,double b){double dx=(b-lon)*111320*Math.cos(Math.toRadians(lat)),dy=(a-lat)*111320;return Math.hypot(dx,dy);}
 static double segment(double lat,double lon,LatLong a,LatLong b){double scale=111320*Math.cos(Math.toRadians(lat)),ax=(a.longitude-lon)*scale,ay=(a.latitude-lat)*111320,bx=(b.longitude-lon)*scale,by=(b.latitude-lat)*111320,dx=bx-ax,dy=by-ay,den=dx*dx+dy*dy,t=den==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/den));return Math.hypot(ax+t*dx,ay+t*dy);}
 public void close(){closed=true;prefetch.clear();ahead.shutdownNow();synchronized(foreground){file.close();}synchronized(background){aheadFile.close();}synchronized(cache){cache.clear();}}
}
