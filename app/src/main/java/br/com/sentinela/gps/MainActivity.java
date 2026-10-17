package br.com.sentinela.gps;
import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.content.pm.PackageManager;
public class MainActivity extends Activity {
 WebView web;
 @Override public void onCreate(Bundle b) {
 super.onCreate(b);
 web = new WebView(this); setContentView(web);
 web.getSettings().setJavaScriptEnabled(true);
 web.getSettings().setDomStorageEnabled(true);
 final WebViewAssetLoader loader = new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
 web.setWebViewClient(new WebViewClientCompat() {
 @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return loader.shouldInterceptRequest(request.getUrl()); }
 });
 web.setWebChromeClient(new WebChromeClient() {
 @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
 if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) cb.invoke(origin,true,false);
 else { pending=cb; pendingOrigin=origin; requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100); }
 }
 });
 web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 GeolocationPermissions.Callback pending; String pendingOrigin;
 @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] grants) {
 super.onRequestPermissionsResult(req,permissions,grants);
 if(req==100&&pending!=null){pending.invoke(pendingOrigin,grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED,false);pending=null;}
 }
 @Override public void onBackPressed(){ if(web.canGoBack())web.goBack();else super.onBackPressed(); }
}
