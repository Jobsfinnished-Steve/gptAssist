/*
Copyright (c) 2017-2019 Divested Computing Group
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.woheller69.gptassist;

import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;
import android.webkit.ValueCallback;
import android.net.Uri;

import androidx.webkit.URLUtilCompat;

import org.woheller69.freeDroidWarn.FreeDroidWarn;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private WebView chatWebView = null;
    private ImageButton restrictedButton = null;
    private WebSettings chatWebSettings = null;
    private CookieManager chatCookieManager = null;
    private final Context context = this;
    private SwipeTouchListener swipeTouchListener;
    private String TAG ="gptAssist";
    private String urlToLoad = "https://chatgpt.com/";
    private static boolean restricted = true;

    private static final ArrayList<String> allowedDomains = new ArrayList<String>();

    private PhotoUploadCoordinator uploadCoordinator;
    private android.content.SharedPreferences preferences;
    private static final String PREF_CONTEXT = "photo_context_enabled";
    private static final String PREF_GPS = "photo_gps_enabled";
    private static final String PREF_GPS_EXPLAINED = "photo_gps_explained";
    private static final int MEDIA_LOCATION_PERMISSION_CODE = 8102;
    private ValueCallback<Uri[]> pendingPhotoCallback;
    private WebChromeClient.FileChooserParams pendingPhotoParams;

    @Override
    protected void onPause() {
        if (chatCookieManager!=null) chatCookieManager.flush();
        swipeTouchListener = null;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (restricted) restrictedButton.setImageDrawable(getDrawable(R.drawable.restricted));
        else restrictedButton.setImageDrawable(getDrawable(R.drawable.unrestricted));

        restrictedButton.setOnClickListener(v -> {
            restricted = !restricted;
            if (restricted) {
                restrictedButton.setImageDrawable(getDrawable(R.drawable.restricted));
                Toast.makeText(context,R.string.urls_restricted,Toast.LENGTH_SHORT).show();
                chatWebSettings.setUserAgentString(modUserAgent());
            }
            else {
                restrictedButton.setImageDrawable(getDrawable(R.drawable.unrestricted));
                Toast.makeText(context,R.string.all_urls,Toast.LENGTH_SHORT).show();
                chatWebSettings.setUserAgentString(modUserAgent()); //Needed for login via Google
            }
            chatWebView.reload();
        });

        swipeTouchListener = new SwipeTouchListener(context) {
            public void onSwipeBottom() {
                if (!chatWebView.canScrollVertically(0)) {
                    restrictedButton.setVisibility(View.VISIBLE);
                }
            }
            public void onSwipeTop(){
                    restrictedButton.setVisibility(View.GONE);
            }
        };

        chatWebView.setOnTouchListener(swipeTouchListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        restricted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create the WebView
        chatWebView = findViewById(R.id.chatWebView);
        registerForContextMenu(chatWebView);
        restrictedButton = findViewById(R.id.restricted);
        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        uploadCoordinator = new PhotoUploadCoordinator(this);
        new PhotoContextFileStore(this).cleanup();

        //Set cookie options
        chatCookieManager = CookieManager.getInstance();
        chatCookieManager.setAcceptCookie(true);
        chatCookieManager.setAcceptThirdPartyCookies(chatWebView, false);

        //Restrict what gets loaded
        initURLs();
        registerForContextMenu(chatWebView);

        chatWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.message().contains("NotAllowedError: Write permission denied.")) {  //this error occurs when user copies to clipboard
                    Toast.makeText(context, R.string.error_copy,Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (needsPhotoLocationPermission(fileChooserParams.getAcceptTypes())) {
                    if (pendingPhotoCallback != null) pendingPhotoCallback.onReceiveValue(null);
                    pendingPhotoCallback = filePathCallback;
                    pendingPhotoParams = fileChooserParams;
                    explainAndRequestPhotoLocation();
                    return true;
                }
                return uploadCoordinator.show(filePathCallback, fileChooserParams);
            }

            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (request.getResources().length > 0 && request.getResources()[0].equals(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            request.grant(request.getResources());
                        } else {
                            // Request the permission from the user
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 123);
                            // A more robust solution would involve storing 'request' and calling request.grant() in onRequestPermissionsResult.
                        }
                    } else {
                        request.deny();
                    }
                } else {
                    request.grant(request.getResources()); // For older Android versions, permissions are granted at install time
                }
            }

        });  //needed to share link

        chatWebView.setWebViewClient(new WebViewClient() {
            //Keep these in sync!
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
                if (!restricted) return null;

                if (request.getUrl().toString().equals("about:blank")) {
                    return null;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldInterceptRequest][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    return new WebResourceResponse("text/javascript", "UTF-8", null); //Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (request.getUrl().getHost().endsWith(url)) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldInterceptRequest][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    if (request.getUrl().getHost().equals("login.microsoftonline.com") || request.getUrl().getHost().equals("accounts.google.com") || request.getUrl().getHost().equals("appleid.apple.com")){
                        // ✅ Post ALL UI/WebView operations to main thread
                        view.post(() -> {
                            Toast.makeText(context, context.getString(R.string.error_microsoft_google), Toast.LENGTH_LONG).show();
                            resetChat(); // Now safe: runs on UI thread where WebView was created
                        });
                    }
                    if (request.getUrl().toString().contains("gravatar.com/avatar/")) {
                        AssetManager assetManager = getAssets();
                        try {
                            InputStream inputStream = assetManager.open("avatar.png");
                            return new WebResourceResponse("image/png","UTF-8",inputStream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return new WebResourceResponse("text/javascript", "UTF-8", null); //Deny URLs not on ALLOWLIST
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!restricted) return false;

                if (request.getUrl().toString().equals("about:blank")) {
                    return false;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    return true; //Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (request.getUrl().getHost().endsWith(url)) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    if (request.getUrl().getHost().equals("login.microsoftonline.com") || request.getUrl().getHost().equals("accounts.google.com") || request.getUrl().getHost().equals("appleid.apple.com")){
                        // ✅ Post ALL UI/WebView operations to main thread
                        view.post(() -> {
                            Toast.makeText(context, context.getString(R.string.error_microsoft_google), Toast.LENGTH_LONG).show();
                            resetChat(); // Now safe: runs on UI thread where WebView was created
                        });
                    }
                    return true; //Deny URLs not on ALLOWLIST
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    logMainFrameError(request, error);
                }
            }
        });

        chatWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Uri source = Uri.parse(url);
            Log.d(TAG,"DownloadManager: " + url);
            DownloadManager.Request request = new DownloadManager.Request(source);
            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
            request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
            request.addRequestHeader("Referer", url);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
            String filename = URLUtilCompat.getFilenameFromContentDisposition(contentDisposition);
            if (filename == null) filename = URLUtilCompat.guessFileName(url, contentDisposition, mimetype);  // only if getFilenameFromContentDisposition does not work and returns null
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            Toast.makeText(this, getString(R.string.download) + "\n" + filename, Toast.LENGTH_SHORT).show();
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            assert dm != null;
            dm.enqueue(request);
        });

        //Set more options
        chatWebSettings = chatWebView.getSettings();
        //Enable some WebView features
        chatWebSettings.setJavaScriptEnabled(true);
        chatWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        chatWebSettings.setDomStorageEnabled(true);
        //Disable some WebView features
        chatWebSettings.setAllowContentAccess(false);
        chatWebSettings.setAllowFileAccess(false);
        chatWebSettings.setBuiltInZoomControls(false);
        chatWebSettings.setDatabaseEnabled(false);
        chatWebSettings.setDisplayZoomControls(false);
        chatWebSettings.setSaveFormData(false);
        chatWebSettings.setGeolocationEnabled(false);

        //Load ChatGPT
        chatWebView.loadUrl(urlToLoad);
        FreeDroidWarn.showWarningOnUpgrade(this, BuildConfig.VERSION_CODE);
        if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this,"https://github.com/woheller69/gptassist");
    }

    @Override
    protected void onDestroy() {
        if (pendingPhotoCallback != null) pendingPhotoCallback.onReceiveValue(null);
        pendingPhotoCallback = null;
        if (uploadCoordinator != null) uploadCoordinator.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Credit (CC BY-SA 3.0): https://stackoverflow.com/a/6077173
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (chatWebView.canGoBack() && !chatWebView.getUrl().equals("about:blank")) {
                        chatWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void resetChat()  {

        chatWebView.clearCache(true);
        chatWebView.clearFormData();
        chatWebView.clearHistory();
        chatWebView.clearMatches();
        chatWebView.clearSslPreferences();
        chatCookieManager.removeSessionCookie();
        chatCookieManager.removeAllCookie();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        chatWebView.loadUrl(urlToLoad);


    }

    private static void initURLs() {
        //Allowed Domains
        allowedDomains.add("cdn.auth0.com");
        allowedDomains.add("auth.openai.com");
        allowedDomains.add("chatgpt.com");
        allowedDomains.add("openai.com");
        allowedDomains.add("fileserviceuploadsperm.blob.core.windows.net");
        allowedDomains.add("cdn.oaistatic.com");
        allowedDomains.add("oaiusercontent.com");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (uploadCoordinator.handlesRequestCode(requestCode)) {
            boolean includeGps = preferences.getBoolean(PREF_GPS, true)
                    && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    || checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED);
            uploadCoordinator.onActivityResult(requestCode, resultCode, intent,
                    preferences.getBoolean(PREF_CONTEXT, true), includeGps);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuItem contextToggle = menu.add(R.string.photo_context_setting).setCheckable(true)
                .setChecked(preferences.getBoolean(PREF_CONTEXT, true));
        contextToggle.setOnMenuItemClickListener(item -> {
            preferences.edit().putBoolean(PREF_CONTEXT, !item.isChecked()).apply(); return true;
        });
        MenuItem gpsToggle = menu.add(R.string.photo_gps_setting).setCheckable(true)
                .setChecked(preferences.getBoolean(PREF_GPS, true));
        gpsToggle.setOnMenuItemClickListener(item -> {
            preferences.edit().putBoolean(PREF_GPS, !item.isChecked()).apply(); return true;
        });
        menu.add(R.string.photo_context_insert).setOnMenuItemClickListener(item -> {
            String last = uploadCoordinator.getLastContext();
            if (last == null) Toast.makeText(this, R.string.photo_context_unavailable, Toast.LENGTH_SHORT).show();
            else ChatGptComposerBridge.append(chatWebView, last);
            return true;
        });
        WebView.HitTestResult result = chatWebView.getHitTestResult();
        String url = "";
        if (result.getExtra() != null) {
            if (result.getType() == IMAGE_TYPE) {
                url = result.getExtra();
                Uri source = Uri.parse(url);
                DownloadManager.Request request = new DownloadManager.Request(source);
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
                request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
                request.addRequestHeader("Referer", url);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                String filename = URLUtil.guessFileName(url, null, "image/jpeg");
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                Toast.makeText(this, getString(R.string.download) + "\n" + filename, Toast.LENGTH_SHORT).show();
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                assert dm != null;
                dm.enqueue(request);
            } else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE || result.getType() == SRC_ANCHOR_TYPE) {
                if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
                    // Create a background thread that has a Looper
                    HandlerThread handlerThread = new HandlerThread("HandlerThread");
                    handlerThread.start();
                    // Create a handler to execute tasks in the background thread.
                    Handler backgroundHandler = new Handler(handlerThread.getLooper());
                    Message msg = backgroundHandler.obtainMessage();
                    chatWebView.requestFocusNodeHref(msg);
                    url = (String) msg.getData().get("url");
                    Toast.makeText(this, "SRC_IMAGE: " + url, Toast.LENGTH_SHORT).show();
                } else if (result.getType() == SRC_ANCHOR_TYPE) {
                    url = result.getExtra();
                    Toast.makeText(this, "SRC_ANCHOR: " + url, Toast.LENGTH_SHORT).show();
                }
                String host = Uri.parse(url).getHost();
                if (host != null) {
                    boolean allowed = false;
                    for (String domain : allowedDomains) {
                        if (host.endsWith(domain)) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) {  //Copy URLs that are not allowed to open to clipboard
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.app_name), url);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    public String modUserAgent(){

        String newPrefix = "Mozilla/5.0 (X11; Linux "+ System.getProperty("os.arch") +")";

        String newUserAgent=WebSettings.getDefaultUserAgent(context);
        String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);
         try {
                newUserAgent=newUserAgent.replace(prefix,newPrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
         return newUserAgent;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Microphone permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == MEDIA_LOCATION_PERMISSION_CODE) launchPendingPhotoChooser();
    }

    private boolean needsPhotoLocationPermission(String[] acceptTypes) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !preferences.getBoolean(PREF_CONTEXT, true)
                || !preferences.getBoolean(PREF_GPS, true)
                || checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED) return false;
        boolean image = false;
        if (acceptTypes != null) for (String type : acceptTypes) if (type != null && type.startsWith("image/")) image = true;
        return image;
    }

    private void explainAndRequestPhotoLocation() {
        if (!preferences.getBoolean(PREF_GPS_EXPLAINED, false)) {
            preferences.edit().putBoolean(PREF_GPS_EXPLAINED, true).apply();
            new AlertDialog.Builder(this).setMessage(R.string.photo_gps_explanation)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> requestPermissions(
                            new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, MEDIA_LOCATION_PERMISSION_CODE))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> launchPendingPhotoChooser())
                    .setOnCancelListener(dialog -> launchPendingPhotoChooser()).show();
        } else requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, MEDIA_LOCATION_PERMISSION_CODE);
    }

    private void launchPendingPhotoChooser() {
        ValueCallback<Uri[]> callback = pendingPhotoCallback;
        WebChromeClient.FileChooserParams params = pendingPhotoParams;
        pendingPhotoCallback = null;
        pendingPhotoParams = null;
        if (callback != null && params != null) uploadCoordinator.show(callback, params);
    }

    private void logMainFrameError(WebResourceRequest request, WebResourceError error) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.w(TAG, "[onReceivedError] " + error.getErrorCode() + ": " + error.getDescription() + " @ " + request.getUrl());
        } else {
            Log.w(TAG, "[onReceivedError] Main-frame load failed @ " + request.getUrl());
        }
    }

}
