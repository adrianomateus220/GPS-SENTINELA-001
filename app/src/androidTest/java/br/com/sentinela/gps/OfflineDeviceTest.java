package br.com.sentinela.gps;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import static org.junit.Assert.*;
import android.location.Location;
import android.os.SystemClock;
import android.view.*;
import android.widget.TextView;
import java.io.File;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.datastore.*;
import org.mapsforge.core.model.*;
import org.mapsforge.core.util.MercatorProjection;
@RunWith(AndroidJUnit4.class)
public class OfflineDeviceTest {
 private android.app.Instrumentation getInstrumentation(){return InstrumentationRegistry.getInstrumentation();}
 private void runTestOnUiThread(Runnable r){getInstrumentation().runOnMainSync(r);}
 private boolean text(View v,String expected){if(v instanceof TextView&&((TextView)v).getText().toString().contains(expected))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(text(g.getChildAt(i),expected))return true;}return false;}
 @Test public void testOfflineOsasco()throws Throwable{
  android.content.Context context=getInstrumentation().getTargetContext();
  File f=new File(context.getExternalFilesDir(null),"offline/sudeste.map");assertTrue("Actual regional map must be installed by CI",f.length()>1000000);
  context.getSharedPreferences("offline-v1",0).edit().clear().putString("active","SP").commit();
  MapFile source=new MapFile(f);byte zoom=16;int x=MercatorProjection.longitudeToTileX(-46.7916,zoom),y=MercatorProjection.latitudeToTileY(-23.5329,zoom);
  MapReadResult data=source.readMapData(new Tile(x,y,zoom,256));source.close();
  LatLong selected=null;String street=null;
  try(RoadLookup reader=new RoadLookup(f)){outer:for(Way way:data.ways){if(RoadLookup.tag(way.tags,"highway").isEmpty())continue;for(LatLong[] ring:way.latLongs)for(int i=1;i<ring.length;i++){LatLong a=ring[i-1],b=ring[i];if(RoadLookup.meters(a.latitude,a.longitude,b.latitude,b.longitude)<30)continue;LatLong mid=new LatLong((a.latitude+b.latitude)/2,(a.longitude+b.longitude)/2);RoadLookup.Result r=reader.at(mid.latitude,mid.longitude,5);if(r.matched){selected=mid;street=r.road;break outer;}}}}
  assertNotNull("Read a named real Osasco street offline",selected);final LatLong fix=selected;final String expected=street;
  MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(new android.content.Intent(context,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(10000);
  for(int i=0;i<6;i++){runTestOnUiThread(()->{Location l=new Location("gps");l.setLatitude(fix.latitude);l.setLongitude(fix.longitude);l.setAccuracy(5);l.setTime(System.currentTimeMillis());l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());activity.onLocationChanged(l);});Thread.sleep(2000);}
  runTestOnUiThread(()->{View root=activity.getWindow().getDecorView();assertTrue("Local street displayed",text(root,expected));assertTrue("Municipality resolved from saved polygons",text(root,"Osasco"));assertTrue("Offline package ready",text(root,"consulta local"));assertFalse(text(root,"COORDENADAS"));assertFalse(text(root,"Voz:"));assertFalse(text(root,"Limites de companhias"));});
  android.graphics.Bitmap screenshot=getInstrumentation().getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(context.getExternalFilesDir(null),"offline-test.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  Thread.sleep(17000);
  runTestOnUiThread(()->{assertTrue("Old address expires with stale GPS",text(activity.getWindow().getDecorView(),"Aguardando posição atualizada"));});
 }
}
