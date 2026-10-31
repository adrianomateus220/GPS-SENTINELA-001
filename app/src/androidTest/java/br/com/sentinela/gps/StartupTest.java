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
  Thread.sleep(1100);android.graphics.Bitmap intro=inst.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"intro-0.13.png"))){intro.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  Thread.sleep(2400);inst.runOnMainSync(()->{assertFalse(text(activity.getWindow().getDecorView(),"PULAR ABERTURA"));assertTrue(text(activity.getWindow().getDecorView(),"SENTINELA"));assertTrue(text(activity.getWindow().getDecorView(),"VIA ATUAL"));});
  inst.runOnMainSync(()->{for(int i=0;i<30;i++){Location l=new Location("test");l.setLatitude(-23.5329+i*.00001);l.setLongitude(-46.7916);l.setAccuracy(5);l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());activity.onLocationChanged(l);}activity.onPause();assertTrue(text(activity.getWindow().getDecorView(),"Aguardando posição atualizada"));});
  Thread.sleep(250);inst.runOnMainSync(()->{assertTrue(text(activity.getWindow().getDecorView(),"Aguardando posição atualizada"));activity.onResume();activity.finish();});
 }
}
