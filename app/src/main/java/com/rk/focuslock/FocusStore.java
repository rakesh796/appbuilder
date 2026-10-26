package com.rk.focuslock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

final class FocusStore {
    static final String PREF = "rk_focus_lock";
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static boolean active(Context c) { return prefs(c).getBoolean("active",false); }
    static void active(Context c, boolean v) { prefs(c).edit().putBoolean("active",v).apply(); }
    static Set<String> allowed(Context c) { return new HashSet<>(prefs(c).getStringSet("allowed",Collections.emptySet())); }
    static void allowed(Context c, Set<String> a) { prefs(c).edit().putStringSet("allowed",new HashSet<>(a)).apply(); }
    static class AppEntry {
        String packageName, label;
        Drawable icon;
        AppEntry(String p, String l, Drawable i) { packageName=p; label=l; icon=i; }
    }
    static List<AppEntry> installedApps(Context ctx) {
        PackageManager pm=ctx.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchers=pm.queryIntentActivities(intent,0);
        LinkedHashMap<String,AppEntry> unique=new LinkedHashMap<>();
        for(ResolveInfo ri:launchers) {
            if (ri.activityInfo==null) continue;
            String p=ri.activityInfo.packageName;
            if (p.equals(ctx.getPackageName()) || unique.containsKey(p)) continue;
            String l=ri.loadLabel(pm).toString();
            try { unique.put(p,new AppEntry(p,l,ri.loadIcon(pm))); }
            catch(Exception ex){ unique.put(p,new AppEntry(p,l,null)); }
        }
        ArrayList<AppEntry> entries=new ArrayList<>(unique.values());
        entries.sort(Comparator.comparing(a->a.label.toLowerCase(java.util.Locale.ROOT)));
        return entries;
    }
    static String iconData(Drawable drawable) {
        if(drawable==null) return "";
        try {
            Bitmap bitmap=Bitmap.createBitmap(70,70,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            Drawable d=drawable.mutate();
            d.setBounds(0,0,70,70);d.draw(canvas);
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
            return "data:image/png;base64,"+Base64.encodeToString(bos.toByteArray(),Base64.NO_WRAP);
        } catch(Exception ignored){return "";}
    }
    static JSONArray visibleAllowed(Context c){
        Set<String> allowed=allowed(c);
        JSONArray apps = new JSONArray();
        for(AppEntry e:installedApps(c)){
            if(!allowed.contains(e.packageName)) continue;
            JSONObject o=new JSONObject();
            try {o.put("name",e.label);o.put("pkg",e.packageName);o.put("icon",iconData(e.icon));}catch(Exception ignored){}
            apps.put(o);
        }
        return apps;
    }
}
