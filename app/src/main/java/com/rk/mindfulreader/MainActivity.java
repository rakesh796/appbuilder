package com.rk.mindfulreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Offline Kindle-style HTML wrapper: no network and no broker order permissions. */
public class MainActivity extends Activity {
    private WebView reader;
    private static final int BACKUP_EXPORT = 311;
    private static final int BACKUP_IMPORT = 312;
    private String pendingExport;

    public final class LocalBridge {
        @JavascriptInterface public void exportBackup(String content) {
            if (content == null || content.length() > 2000000) return;
            runOnUiThread(() -> {
                pendingExport = content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "mindful-reader-backup.json");
                startActivityForResult(intent, BACKUP_EXPORT);
            });
        }
        @JavascriptInterface public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, BACKUP_IMPORT);
            });
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        reader = new WebView(this);
        reader.setBackgroundColor(0xFFF5F0E6);
        setContentView(reader);
        WebSettings s = reader.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        reader.addJavascriptInterface(new LocalBridge(), "RKNative");
        reader.setWebChromeClient(new WebChromeClient());
        reader.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !("file".equalsIgnoreCase(req.getUrl().getScheme())
                    && "file:///android_asset/index.html".equals(req.getUrl().toString()));
            }
        });
        reader.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int req, int result, Intent intent) {
        super.onActivityResult(req, result, intent);
        if (result != RESULT_OK || intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        try {
            if (req == BACKUP_EXPORT && pendingExport != null) {
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new Exception("No write stream");
                    output.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                }
                pendingExport = null;
                Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show();
            } else if (req == BACKUP_IMPORT) {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new Exception("No read stream");
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192]; int length;
                    while ((length = input.read(buffer)) != -1) {
                        if (output.size() + length > 2000000) throw new Exception("File exceeds backup limit");
                        output.write(buffer, 0, length);
                    }
                    String json = new String(output.toByteArray(), StandardCharsets.UTF_8);
                    reader.evaluateJavascript("window.receiveNativeBackup(" + JSONObject.quote(json) + ")", null);
                }
            }
        } catch (Exception error) {
            Toast.makeText(this, "Backup failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onBackPressed() {
        if (reader == null) { super.onBackPressed(); return; }
        reader.evaluateJavascript("window.readerBack ? String(window.readerBack()) : 'false'", result -> {
            if ("\"false\"".equals(result) || "false".equals(result)) {
                runOnUiThread(() -> MainActivity.super.onBackPressed());
            }
        });
    }
    @Override protected void onDestroy() {
        if (reader != null) { reader.destroy(); reader = null; }
        super.onDestroy();
    }
}