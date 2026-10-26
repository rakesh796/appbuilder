package com.rk.focuslock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Reversible, best-effort blocker; not a Device Owner kiosk. */
public class FocusAccessibilityService extends AccessibilityService {
    private long lastReturn=0;
    private String homePackage = "";
    private static final Set<String> SYSTEM_EXCEPTIONS = new HashSet<>(Arrays.asList(
        "com.android.systemui", "com.android.settings", "com.android.permissioncontroller",
        "com.google.android.permissioncontroller", "com.google.android.packageinstaller",
        "com.android.packageinstaller", "com.android.phone", "com.google.android.dialer",
        "com.android.dialer", "com.android.server.telecom", "com.android.emergency",
        "com.google.android.inputmethod.latin", "com.android.inputmethod.latin",
        "com.google.android.documentsui", "com.android.documentsui", "android"));
    @Override public void onServiceConnected() { super.onServiceConnected(); resolveHome(); }
    private void resolveHome(){
        try {
            Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info=getPackageManager().resolveActivity(home,0);
            if(info!=null && info.activityInfo!=null)homePackage=info.activityInfo.packageName;
        } catch(Exception ignored){}
    }
    private boolean safeException(String p) {
        return p.equals(getPackageName()) || p.equals(homePackage)
            || SYSTEM_EXCEPTIONS.contains(p)
            || p.startsWith("com.android.inputmethod.") || p.contains("launcher")
            || p.startsWith("com.google.android.apps.nexuslauncher");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event==null || !FocusStore.active(this))return;
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType()!=AccessibilityEvent.TYPE_WINDOWS_CHANGED)return;
        CharSequence pkg=event.getPackageName();if(pkg==null)return;
        String p=pkg.toString();
        if(safeException(p) || FocusStore.allowed(this).contains(p))return;
        long now=SystemClock.elapsedRealtime();
        if(now-lastReturn<800)return;
        lastReturn=now;
        performGlobalAction(GLOBAL_ACTION_HOME);
    }
    @Override public void onInterrupt(){}
}
