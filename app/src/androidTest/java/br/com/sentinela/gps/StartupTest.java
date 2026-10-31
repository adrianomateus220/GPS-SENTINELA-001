package br.com.sentinela.gps;
import org.junit.Test;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.*;
import android.widget.TextView;
import android.location.Location;
import android.os.SystemClock;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StartupTest {
 private boolean text(View v,String s){if(v instanceof TextView&&((TextView)v).getText().toString().contains(s))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(text(((ViewGroup)v).getChildAt(i),s))return true;return false;}
 @Test public void introAndGpsLifecycle()throws Exception{
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Context context=inst.getTargetContext();
  for(String perm:new String[]{"ACCESS_FINE_LOCATION","ACCESS_COARSE_LOCATION"})try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("pm grant "+context.getPackageName()+" android.permission."+perm);java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
  MainActivity activity=(MainActivity)inst.startActivitySync(new android.content.Intent(context,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
  inst.runOnMainSync(()->assertTrue(text(activity.getWindow().getDecorView(),"PULAR ABERTURA")));
  // GPS callbacks are processed while the animation is still present.
  inst.runOnMainSync(()->{Location l=new Location("test");l.setLatitude(-23.5329);l.setLongitude(-46.7916);l.setAccuracy(5);l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());l.setTime(System.currentTimeMillis());activity.onLocationChanged(l);assertTrue(text(activity.getWindow().getDecorView(),"Localização ativa"));});
  Thread.sleep(2600);try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("screencap -p /data/local/tmp/sentinela-intro.png");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
  Thread.sleep(IntroView.DURATION_MS);inst.runOnMainSync(()->{assertFalse(text(activity.getWindow().getDecorView(),"PULAR ABERTURA"));assertTrue(text(activity.getWindow().getDecorView(),"SENTINELA"));assertTrue(text(activity.getWindow().getDecorView(),"VIA ATUAL"));assertFalse(text(activity.getWindow().getDecorView(),"REFERÊNCIA VISUAL"));});
  inst.runOnMainSync(()->{for(int i=0;i<30;i++){Location l=new Location("test");l.setLatitude(-23.5329+i*.00001);l.setLongitude(-46.7916);l.setAccuracy(5);l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());activity.onLocationChanged(l);}activity.onPause();assertTrue(text(activity.getWindow().getDecorView(),"Aguardando posição atualizada"));});
  Thread.sleep(250);inst.runOnMainSync(()->{assertTrue(text(activity.getWindow().getDecorView(),"Aguardando posição atualizada"));activity.onResume();activity.finish();});
 }
 @Test public void offlineThemeRenders()throws Exception{
  android.app.Instrumentation inst=InstrumentationRegistry.getInstrumentation();android.content.Context context=inst.getTargetContext();
  for(String perm:new String[]{"ACCESS_FINE_LOCATION","ACCESS_COARSE_LOCATION"})try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("pm grant "+context.getPackageName()+" android.permission."+perm);java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
  java.io.File f=new java.io.File(context.getExternalFilesDir(null),"offline/sudeste.map");f.getParentFile().mkdirs();try(java.io.InputStream in=inst.getContext().getAssets().open("fixture.map");java.io.FileOutputStream out=new java.io.FileOutputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
  org.mapsforge.map.reader.MapFile reader=new org.mapsforge.map.reader.MapFile(f);org.mapsforge.core.model.LatLong midpoint=reader.boundingBox().getCenterPoint();
  byte zoom=12;org.mapsforge.core.model.Tile tile=new org.mapsforge.core.model.Tile(org.mapsforge.core.util.MercatorProjection.longitudeToTileX(midpoint.longitude,zoom),org.mapsforge.core.util.MercatorProjection.latitudeToTileY(midpoint.latitude,zoom),zoom,256);
  org.mapsforge.map.datastore.MapReadResult data=reader.readMapData(tile);assertFalse("Fixture must contain roads",data.ways.isEmpty());org.mapsforge.core.model.LatLong[] points=data.ways.get(0).latLongs[0];assertTrue(points.length>1);org.mapsforge.core.model.LatLong center=new org.mapsforge.core.model.LatLong((points[0].latitude+points[1].latitude)/2,(points[0].longitude+points[1].longitude)/2);reader.close();
  MainActivity activity=(MainActivity)inst.startActivitySync(new android.content.Intent(context,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
  Thread.sleep(IntroView.DURATION_MS+300);
  inst.runOnMainSync(()->{try{activity.onPause();java.lang.reflect.Field lastField=MainActivity.class.getDeclaredField("last");lastField.setAccessible(true);lastField.set(activity,null);java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("showMap",String.class);method.setAccessible(true);assertEquals(true,method.invoke(activity,"SP"));java.lang.reflect.Field field=MainActivity.class.getDeclaredField("map");field.setAccessible(true);org.mapsforge.map.android.view.MapView map=(org.mapsforge.map.android.view.MapView)field.get(activity);map.setCenter(center);map.setZoomLevel((byte)16);android.util.Log.i("SentinelaMapTest","Road center: "+center);}catch(Exception e){throw new AssertionError(e);}});
  Thread.sleep(3000);try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("screencap -p /data/local/tmp/sentinela-map.png");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
  inst.runOnMainSync(()->activity.finish());
 }

}
