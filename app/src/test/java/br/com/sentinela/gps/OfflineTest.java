package br.com.sentinela.gps;
import org.junit.Test;
import org.json.JSONObject;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.reader.MapFile;
import java.io.*;
import static org.junit.Assert.*;
public class OfflineTest {
 @Test public void polygonHolesAndOutside()throws Exception{JSONObject p=new JSONObject("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[10,0],[10,10],[0,10],[0,0]],[[4,4],[6,4],[6,6],[4,6],[4,4]]]}");assertTrue(GeoAreas.contains(p,2,2));assertFalse(GeoAreas.contains(p,5,5));assertFalse(GeoAreas.contains(p,20,5));}
 @Test public void allStatesHavePackages(){for(String uf:GeoAreas.UFS)assertFalse(GeoAreas.region(uf).isEmpty());assertEquals("sudeste",GeoAreas.region("SP"));assertEquals("centro-oeste",GeoAreas.region("DF"));}
 @Test public void streetSegmentNotNearestVertex(){assertEquals(0,RoadLookup.segment(0,0,new LatLong(0,-.001),new LatLong(0,.001)),.01);assertTrue(RoadLookup.segment(.001,0,new LatLong(0,-.001),new LatLong(0,.001))>100);}
 @Test public void mapFileReallyReadsOffline()throws Exception{File f=new File(getClass().getResource("/fixture.map").toURI());MapFile map=new MapFile(f);LatLong center=map.boundingBox().getCenterPoint();assertEquals(f.length(),map.getMapFileInfo().fileSize);map.close();try(RoadLookup reader=new RoadLookup(f)){assertNotNull(reader.at(center.latitude,center.longitude,10));assertFalse(reader.at(center.latitude,center.longitude,100).matched);}}
 @Test public void truncatedMapRejected()throws Exception{File f=File.createTempFile("bad-map",".map");try{try(FileOutputStream out=new FileOutputStream(f)){out.write("mapsforge binary OSM".getBytes());}try{MapFile map=new MapFile(f);map.close();fail("Truncated map accepted");}catch(IllegalArgumentException expected){}}finally{f.delete();}}
}
