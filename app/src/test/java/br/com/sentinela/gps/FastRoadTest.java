package br.com.sentinela.gps;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.core.model.LatLong;
public class FastRoadTest {
 private RoadIndex.Segment road(String name,double a,double b,double c,double d){return new RoadIndex.Segment(name,"Centro",a,b,c,d);}
 @Test public void perpendicularIntersectionUsesHeadingButNoHeadingIsAmbiguous(){List<RoadIndex.Segment> roads=Arrays.asList(road("Norte",-.001,0,.001,0),road("Leste",0,-.001,0,.001));assertFalse(RoadIndex.match(roads,0,0,5,null,"").matched);assertEquals("Norte",RoadIndex.match(roads,0,0,5,0d,"").name);assertEquals("Leste",RoadIndex.match(roads,0,0,5,90d,"Norte").name);}
 @Test public void noStickyRoadAcrossParallelStreet(){List<RoadIndex.Segment> roads=Arrays.asList(road("Antiga",-.001,0,.001,0),road("Nova",-.001,.00018,.001,.00018));assertEquals("Nova",RoadIndex.match(roads,0,.00017,5,0d,"Antiga").name);assertFalse(RoadIndex.match(roads,0,.00009,20,0d,"Antiga").matched);}
 @Test public void boundingBoxesPruneFarSegments(){List<RoadIndex.Segment> roads=new ArrayList<>();roads.add(road("Atual",-.001,0,.001,0));for(int i=0;i<10000;i++)roads.add(road("Longe"+i,1+i*.001,1,1+i*.001,2));RoadIndex.Match m=RoadIndex.match(roads,0,0,5,null,"");assertEquals("Atual",m.name);assertEquals(1,m.examined);assertFalse(RoadIndex.match(roads,0,0,100,null,"").matched);assertFalse(RoadIndex.match(roads,0,0,Double.NaN,null,"").matched);}
 @Test public void newerPositionReplacesBacklog()throws Exception{ExecutorService pool=Executors.newSingleThreadExecutor();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);List<Integer> seen=Collections.synchronizedList(new ArrayList<>());LatestTask<Integer> q=new LatestTask<>(pool,n->{seen.add(n);if(n==1){entered.countDown();try{release.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}else done.countDown();});q.submit(1);assertTrue(entered.await(2,TimeUnit.SECONDS));for(int i=2;i<=100;i++)q.submit(i);release.countDown();assertTrue(done.await(2,TimeUnit.SECONDS));pool.shutdown();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));assertEquals(Arrays.asList(1,100),seen);}
 @Test public void warmQueriesAvoidMapReadsAndSurviveRestart()throws Exception{
  File f=new File(getClass().getResource("/fixture.map").toURI()),cache=java.nio.file.Files.createTempDirectory("road-cache").toFile();MapFile source=new MapFile(f);LatLong center=source.boundingBox().getCenterPoint();source.close();
  try(RoadLookup reader=new RoadLookup(f,cache)){reader.at(center.latitude,center.longitude,5);long reads=reader.mapReads;long start=System.nanoTime();for(int i=0;i<100;i++)reader.at(center.latitude,center.longitude,5);long micros=(System.nanoTime()-start)/1000;assertEquals(reads,reader.mapReads);assertTrue(reader.ramHits>=100);System.out.println("100 warm queries: "+micros+" us; additional map reads: 0");reader.prepare(center.latitude,center.longitude,10,0d);long end=System.currentTimeMillis()+5000;while(!hasBinary(cache)&&System.currentTimeMillis()<end)Thread.sleep(20);assertTrue(hasBinary(cache));}
  try(RoadLookup reader=new RoadLookup(f,cache)){reader.at(center.latitude,center.longitude,5);assertTrue(reader.diskHits>0);}
 }
 private boolean hasBinary(File f){File[] files=f.listFiles();if(files==null)return false;for(File x:files)if(x.isDirectory()?hasBinary(x):x.getName().endsWith(".bin"))return true;return false;}
}
