package org.woheller69.gptassist;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoUploadCoordinator {
    public static final int REQUEST_CODE = 8101;
    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ValueCallback<Uri[]> callback;
    private boolean imageRequest;
    private volatile boolean destroyed;
    private String lastContext;

    public PhotoUploadCoordinator(Activity activity) { this.activity = activity; }

    public boolean show(ValueCallback<Uri[]> next, WebChromeClient.FileChooserParams params) {
        finish(null);
        callback = next;
        imageRequest = acceptsImages(params.getAcceptTypes());
        Intent intent;
        try { intent = params.createIntent(); } catch (RuntimeException e) { intent = new Intent(Intent.ACTION_GET_CONTENT); }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (imageRequest && (intent.getType() == null || "*/*".equals(intent.getType()))) intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);
        try { activity.startActivityForResult(intent, REQUEST_CODE); }
        catch (RuntimeException e) { finish(null); Toast.makeText(activity, R.string.photo_read_error, Toast.LENGTH_SHORT).show(); }
        return true;
    }

    public void onActivityResult(int resultCode, Intent data, boolean contextEnabled, boolean includeGps) {
        if (callback == null) return;
        if (resultCode != Activity.RESULT_OK || data == null) { finish(null); return; }
        List<Uri> selected = selectedUris(data);
        if (selected.isEmpty()) { finish(null); return; }
        if (!imageRequest || !contextEnabled) { finish(selected.toArray(new Uri[0])); return; }
        executor.execute(() -> createResult(selected, includeGps));
    }

    private void createResult(List<Uri> selected, boolean includeGps) {
        PhotoMetadataReader reader = new PhotoMetadataReader(activity);
        List<PhotoContext> contexts = new ArrayList<>();
        boolean hadError = false;
        for (Uri uri : selected) {
            try { contexts.add(reader.read(uri, includeGps)); }
            catch (RuntimeException e) { contexts.add(PhotoContext.unknown()); hadError = true; }
        }
        String text = PhotoContextFormatter.format(contexts);
        Uri contextUri;
        try { contextUri = new PhotoContextFileStore(activity).write(text); }
        catch (Exception e) {
            boolean finalHadError = true;
            main.post(() -> { if (!destroyed) { lastContext = text; finish(selected.toArray(new Uri[0])); warn(finalHadError); } });
            return;
        }
        ArrayList<Uri> result = new ArrayList<>(selected);
        result.add(contextUri);
        boolean finalHadError = hadError;
        main.post(() -> { if (!destroyed) { lastContext = text; finish(result.toArray(new Uri[0])); warn(finalHadError); } });
    }

    private void warn(boolean error) {
        if (error) Toast.makeText(activity, R.string.photo_partial_metadata_error, Toast.LENGTH_SHORT).show();
    }
    public String getLastContext() { return lastContext; }
    public void destroy() { destroyed = true; finish(null); executor.shutdownNow(); }

    private void finish(Uri[] value) {
        ValueCallback<Uri[]> current = callback;
        callback = null;
        if (current != null) current.onReceiveValue(value);
    }
    private static List<Uri> selectedUris(Intent intent) {
        ArrayList<Uri> result = new ArrayList<>();
        ClipData clip = intent.getClipData();
        if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) {
            Uri uri = clip.getItemAt(i).getUri(); if (uri != null) result.add(uri);
        }
        else if (intent.getData() != null) result.add(intent.getData());
        return result;
    }
    private static boolean acceptsImages(String[] types) {
        if (types == null || types.length == 0) return false;
        for (String type : types) if (type != null && (type.startsWith("image/") || "image/*".equals(type))) return true;
        return false;
    }
}
