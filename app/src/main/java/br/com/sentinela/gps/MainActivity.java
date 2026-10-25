package br.com.sentinela.gps;
import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import java.util.Locale;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
public class MainActivity extends Activity {
 private WebView web;
 private final ExecutorService geocoderWorker=Executors.newSingleThreadExecutor();
 private final AtomicBoolean geocoderBusy=new AtomicBoolean(false);
 private WebResourceResponse nativeAddress(Uri uri){
  JSONObject result=new JSONObject();
  if(hasLocation()&&Geocoder.isPresent()&&geocoderBusy.compareAndSet(false,true)){
   Future<JSONObject> future=null;
   try{
    final double lat=Double.parseDouble(uri.getQueryParameter("lat"));
    final double lon=Double.parseDouble(uri.getQueryParameter("lon"));
    if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IllegalArgumentException();
    future=geocoderWorker.submit(()->{
     try{
      JSONObject answer=new JSONObject();
      List<Address> matches=new Geocoder(MainActivity.this,new Locale("pt","BR")).getFromLocation(lat,lon,1);
      if(matches!=null&&!matches.isEmpty()){
       Address a=matches.get(0);
       answer.put("road",a.getThoroughfare());answer.put("house_number",a.getSubThoroughfare());
       answer.put("suburb",a.getSubLocality());answer.put("city",a.getLocality());answer.put("state",a.getAdminArea());
       if(a.hasLatitude()&&a.hasLongitude()){answer.put("lat",a.getLatitude());answer.put("lon",a.getLongitude());}
      }
      return answer;
     }finally{geocoderBusy.set(false);}
    });
    result=future.get(3,TimeUnit.SECONDS);
   }catch(Exception ignored){if(future==null)geocoderBusy.set(false);}
  }
  return new WebResourceResponse("application/json","UTF-8",new ByteArrayInputStream(result.toString().getBytes(StandardCharsets.UTF_8)));
 }
 private GeolocationPermissions.Callback pending;
 private String pendingOrigin;
 private static final String ORIGIN="https://appassets.androidplatform.net";
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  web=new WebView(this);setContentView(web);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  final WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClientCompat(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){Uri uri=request.getUrl();
    if("https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&"/native-address".equals(uri.getPath()))return nativeAddress(uri);
    return loader.shouldInterceptRequest(uri);}
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    if(!request.isForMainFrame())return false;
    Uri uri=request.getUrl();
    if("https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost()))return false;
    if("https".equals(uri.getScheme())||"tel".equals(uri.getScheme())){
     try{startActivity(new Intent("tel".equals(uri.getScheme())?Intent.ACTION_DIAL:Intent.ACTION_VIEW,uri));}
     catch(ActivityNotFoundException e){Toast.makeText(MainActivity.this,"Nenhum aplicativo disponível para abrir este destino",Toast.LENGTH_LONG).show();}
    }
    return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback cb){
    if(!ORIGIN.equals(origin)&&!(ORIGIN+"/").equals(origin)){cb.invoke(origin,false,false);return;}
    if(hasLocation()){cb.invoke(origin,true,false);return;}
    if(pending!=null)pending.invoke(pendingOrigin,false,false);
    pending=cb;pendingOrigin=origin;
    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
   }
  });
  web.loadUrl(ORIGIN+"/assets/index.html");
 }
 private boolean hasLocation(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] grants){super.onRequestPermissionsResult(req,permissions,grants);if(req==100&&pending!=null){pending.invoke(pendingOrigin,hasLocation(),false);pending=null;}}
 @Override protected void onPause(){super.onPause();web.onPause();}
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();}
 @Override protected void onDestroy(){if(pending!=null){pending.invoke(pendingOrigin,false,false);pending=null;}geocoderWorker.shutdownNow();web.destroy();super.onDestroy();}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
