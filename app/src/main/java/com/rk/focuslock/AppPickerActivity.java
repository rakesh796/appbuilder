package com.rk.focuslock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppPickerActivity extends Activity {
    private Set<String> selection;
    private List<FocusStore.AppEntry> apps;
    private LinearLayout list;
    private static final Set<String> SCREENSHOT_SUGGESTIONS=new HashSet<>(Arrays.asList(
        "com.adobe.reader", "com.google.android.gm", "com.google.android.apps.maps",
        "com.estrongs.android.pop", "com.phonepe.app", "com.google.android.apps.photos",
        "com.google.android.dialer", "com.android.dialer", "com.whatsapp",
        "com.whatsapp.w4b", "com.openai.chatgpt", "com.anthropic.claude",
        "com.android.chrome", "com.microsoft.emmx", "com.google.android.apps.docs"));
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        selection=new HashSet<>(FocusStore.allowed(this));
        apps=FocusStore.installedApps(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(17),dp(20),dp(17),dp(12));root.setBackgroundColor(Color.rgb(11,9,21));
        TextView title=new TextView(this);title.setText("CHOOSE ALLOWED APPS");title.setTextColor(Color.rgb(255,217,148));title.setTextSize(23);title.setTypeface(null,1);root.addView(title);
        TextView help=new TextView(this);help.setText("Select only the apps you need. Unselected apps will be redirected to Home while Focus Lock is active. Android system and emergency access remain available.");help.setTextColor(Color.rgb(205,196,198));help.setTextSize(13);help.setPadding(0,dp(8),0,dp(12));root.addView(help);
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search your installed apps");search.setHintTextColor(Color.rgb(139,128,145));search.setTextColor(Color.WHITE);root.addView(search);
        Button suggested=new Button(this);suggested.setText("SUGGEST KNOWN APPS FROM SCREENSHOTS");suggested.setTextSize(11);suggested.setTextColor(Color.rgb(250,221,170));suggested.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(51,36,48)));root.addView(suggested);
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button save=new Button(this);save.setText("SAVE APPROVED APPS ("+selection.size()+")");save.setTextColor(Color.BLACK);save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(247,195,105)));
        save.setOnClickListener(v->{FocusStore.allowed(this,selection);finish();});root.addView(save);
        setContentView(root);
        Runnable render=()->{String filter=search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);list.removeAllViews();
            for(FocusStore.AppEntry app:apps){if(!app.label.toLowerCase(java.util.Locale.ROOT).contains(filter))continue;
                LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(3),dp(8),dp(3),dp(8));
                if(app.icon!=null){ImageView icon=new ImageView(this);icon.setImageDrawable(app.icon);row.addView(icon,new LinearLayout.LayoutParams(dp(37),dp(37)));}
                CheckBox box=new CheckBox(this);box.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(247,195,105)));box.setText(app.label);box.setTextColor(Color.WHITE);box.setTextSize(15);box.setChecked(selection.contains(app.packageName));
                box.setPadding(dp(8),0,0,0);box.setOnCheckedChangeListener((c,on)->{if(on)selection.add(app.packageName);else selection.remove(app.packageName);save.setText("SAVE APPROVED APPS ("+selection.size()+")");});
                row.addView(box,new LinearLayout.LayoutParams(0,dp(56),1));row.setOnClickListener(v->box.setChecked(!box.isChecked()));list.addView(row);
            }
        };
        suggested.setOnClickListener(v->{
            int added=0;
            for(FocusStore.AppEntry e:apps){if(SCREENSHOT_SUGGESTIONS.contains(e.packageName) && selection.add(e.packageName))added++;}
            android.widget.Toast.makeText(this,added+" suggested apps added. Review the entire selection before saving.",android.widget.Toast.LENGTH_LONG).show();
            save.setText("SAVE APPROVED APPS ("+selection.size()+")");render.run();
        });
        render.run();search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){render.run();}public void afterTextChanged(Editable e){}});
    }
}