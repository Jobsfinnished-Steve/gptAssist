/*
Copyright (c) 2017-2019 Divested Computing Group
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
*/
package org.woheller69.gptassist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class MainActivity extends Activity implements PhotoUploadCoordinator.Delegate {
    private static final String TAG = "gptAssist";
    private static final String CHATGPT_URL = "https://chatgpt.com/";
    private static final String PREF_PROXY_MIME = "upload_proxy_mime";

    private WebView chatWebView;
    private PhotoUploadCoordinator uploadCoordinator;
    private android.content.SharedPreferences preferences;
    private UploadProxyDiagnostics lastProxyDiagnostics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatWebView = findViewById(R.id.chatWebView);
        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        uploadCoordinator = new PhotoUploadCoordinator(this, this);
        new UploadProxyStore(this).cleanup();
        registerForContextMenu(chatWebView);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        // Cloudflare and federated authentication can require cookies across related hosts.
        cookies.setAcceptThirdPartyCookies(chatWebView, true);

        WebSettings settings = chatWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setGeolocationEnabled(false);

        chatWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                return uploadCoordinator.show(callback, params);
            }
        });

        // Deliberately unrestricted: Cloudflare/auth resources must not be filtered by an app allowlist.
        chatWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.w(TAG, "Main-frame load failed: " + error.getErrorCode());
                    } else {
                        Log.w(TAG, "Main-frame load failed");
                    }
                }
            }
        });

        chatWebView.loadUrl(CHATGPT_URL);
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (uploadCoordinator != null) uploadCoordinator.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (uploadCoordinator.handlesRequestCode(requestCode)) {
            UploadProxyMimeMode mode = UploadProxyMimeMode.fromPreference(
                    preferences.getString(PREF_PROXY_MIME, UploadProxyMimeMode.TEXT.name()));
            uploadCoordinator.onActivityResult(requestCode, resultCode, intent, mode);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, android.view.View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        UploadProxyMimeMode mode = UploadProxyMimeMode.fromPreference(
                preferences.getString(PREF_PROXY_MIME, UploadProxyMimeMode.TEXT.name()));
        menu.add(getString(R.string.upload_proxy_mime, mode.name())).setOnMenuItemClickListener(item -> {
            UploadProxyMimeMode next = mode.next();
            preferences.edit().putString(PREF_PROXY_MIME, next.name()).apply();
            Toast.makeText(this, getString(R.string.upload_proxy_mime, next.name()), Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.add(R.string.upload_proxy_diagnostics).setOnMenuItemClickListener(item -> {
            showUploadProxyDiagnostics();
            return true;
        });
    }

    private void showUploadProxyDiagnostics() {
        UploadProxyDiagnostics d = lastProxyDiagnostics;
        String message = d == null ? getString(R.string.upload_proxy_no_diagnostics)
                : "Proxy mode: " + d.mode.name()
                + "\nSelected image count: " + d.selectedCount
                + "\nProxy count: " + d.proxyCount
                + "\nReported MIME: " + d.reportedMime
                + "\nBytes identical: " + (d.bytesIdentical ? "YES" : "NO")
                + "\nOriginal access: " + d.originalAccessStatus.name()
                + "\nProxy GPS coordinates readable: " + diagnosticBoolean(d.gpsCoordinatesReadable)
                + "\nProxy GPS altitude readable: " + diagnosticBoolean(d.gpsAltitudeReadable)
                + "\nWebView callback: " + (d.callbackCompleted ? "COMPLETED" : "FAILED");
        new AlertDialog.Builder(this).setTitle(R.string.upload_proxy_diagnostics)
                .setMessage(message).setPositiveButton(android.R.string.ok, null).show();
    }

    private static String diagnosticBoolean(Boolean value) {
        return value == null ? "NOT_CHECKED" : (value ? "YES" : "NO");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN
                && chatWebView.canGoBack()) {
            chatWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onUploadProxyDiagnostics(UploadProxyDiagnostics diagnostics) {
        lastProxyDiagnostics = diagnostics;
    }
}
