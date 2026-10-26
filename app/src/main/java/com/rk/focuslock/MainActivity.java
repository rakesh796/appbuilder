package com.rk.focuslock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private boolean serviceAvailable(){
        AccessibilityManager mgr=(AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if(mgr==null)return false;
        for(AccessibilityServiceInfo i:mgr.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
            if(i.getResolveInfo()!=null && i.getResolveInfo().serviceInfo!=null
                    && getPackageName().equals(i.getResolveInfo().serviceInfo.packageName)
                    && i.getResolveInfo().serviceInfo.name.endsWith("FocusAccessibilityService"))return true;
        }
        return false;
    }
    private String state(){
        JSONObject o=new JSONObject();
        boolean enabled=serviceAvailable();
        if(!enabled && FocusStore.active(this)) FocusStore.active(this,false);
        try{o.put("active",FocusStore.active(this));o.put("enabled",enabled);
            o.put("apps",FocusStore.visibleAllowed(this));o.put("total",FocusStore.allowed(this).size());}catch(Exception ignored){}
        return o.toString();
    }
    private void sync(){if(web!=null)web.evaluateJavascript("window.refreshFocus && window.refreshFocus()",null);}
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        web=new WebView(this);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setAllowFileAccess(true);s.setDomStorageEnabled(false);
        s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
            String url=request.getUrl().toString();return !url.startsWith("file:///android_asset/");
        }});
        web.addJavascriptInterface(new Bridge(),"RKFocus");
        web.setBackgroundColor(0xff050512);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onResume(){super.onResume();if(web!=null)web.postDelayed(this::sync,320);}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("RKFocus");web.destroy();web=null;}super.onDestroy();}
    private class Bridge {
        @JavascriptInterface public String getState(){return state();}
        @JavascriptInterface public void manage(){runOnUiThread(()->startActivity(new Intent(MainActivity.this,AppPickerActivity.class)));}
        @JavascriptInterface public void launch(String pkg){runOnUiThread(()->{
            if(!FocusStore.allowed(MainActivity.this).contains(pkg)){Toast.makeText(MainActivity.this,"App not approved",Toast.LENGTH_SHORT).show();return;}
            Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
            if(i==null){Toast.makeText(MainActivity.this,"App not available",Toast.LENGTH_SHORT).show();return;}
            try{startActivity(i);}catch(Exception ex){Toast.makeText(MainActivity.this,"Cannot open app",Toast.LENGTH_SHORT).show();}
        });}
        @JavascriptInterface public void toggle(){runOnUiThread(()->{
            if(FocusStore.active(MainActivity.this)){FocusStore.active(MainActivity.this,false);Toast.makeText(MainActivity.this,"Focus Lock revoked",Toast.LENGTH_SHORT).show();sync();return;}
            if(FocusStore.allowed(MainActivity.this).isEmpty()){
                new AlertDialog.Builder(MainActivity.this).setTitle("Choose allowed apps first").setMessage("Choose the apps you need before turning on Focus Lock.")
                    .setPositiveButton("CHOOSE APPS",(d,w)->startActivity(new Intent(MainActivity.this,AppPickerActivity.class))).setNegativeButton("CANCEL",null).show();return;
            }
            if(!serviceAvailable()){
                new AlertDialog.Builder(MainActivity.this).setTitle("Enable accessibility blocker")
                    .setMessage("Android must allow RK Focus Lock to identify which app is opened and return to Home when it is not approved. This does not read your screen content. You can revoke access at any time in Settings. After enabling the service, return here and tap ACTIVATE LOCK again.")
                    .setPositiveButton("OPEN SETTINGS",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton("CANCEL",null).show();return;
            }
            new AlertDialog.Builder(MainActivity.this).setTitle("Activate app blocking?")
                .setMessage("Only the apps you selected will open freely. Unapproved apps will return to Home. Android settings, system UI and emergency access remain available. You can stop the lock any time using REVOKE LOCK.")
                .setPositiveButton("ACTIVATE",(d,w)->{FocusStore.active(MainActivity.this,true);sync();})
                .setNegativeButton("CANCEL",null).show();
        });}
        @JavascriptInterface public void home(){runOnUiThread(()->{
            if(Build.VERSION.SDK_INT>=29){RoleManager rm=getSystemService(RoleManager.class);
                if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)){
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),44);return;
                }
            }
            try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){Toast.makeText(MainActivity.this,"Set RK Focus Lock as your Home app in Android Settings",Toast.LENGTH_LONG).show();}
        });}
        @JavascriptInterface public void restoreHome(){runOnUiThread(()->{
            FocusStore.active(MainActivity.this,false);sync();
            try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){Toast.makeText(MainActivity.this,"Open Settings > Default apps > Home app",Toast.LENGTH_LONG).show();}
        });}
    }
}
