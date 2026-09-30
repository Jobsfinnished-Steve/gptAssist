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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Image-proxy file chooser; generic non-image files pass through unchanged. */
public final class PhotoUploadCoordinator {
    public interface Delegate {
        void onUploadProxyDiagnostics(UploadProxyDiagnostics diagnostics);
    }

    private static final int REQUEST_CODE_BASE = 8100;
    private static final int REQUEST_CODE_COUNT = 50000;
    private final Activity activity;
    private final Delegate delegate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CallbackRequestRegistry<Uri[]> requests = new CallbackRequestRegistry<>();
    private CallbackRequestRegistry.Request<Uri[]> currentRequest;
    private int currentRequestCode;
    private volatile boolean destroyed;

    public PhotoUploadCoordinator(Activity activity, Delegate delegate) {
        this.activity = activity;
        this.delegate = delegate;
    }

    public boolean show(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        currentRequest = requests.begin(callback::onReceiveValue);
        currentRequestCode = REQUEST_CODE_BASE + 1
                + (int) ((currentRequest.getGeneration() - 1) % REQUEST_CODE_COUNT);
        Intent intent;
        try {
            intent = params.createIntent();
        } catch (RuntimeException e) {
            intent = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);
        try {
            activity.startActivityForResult(intent, currentRequestCode);
        } catch (RuntimeException e) {
            requests.complete(currentRequest, null);
            Toast.makeText(activity, R.string.photo_read_error, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public boolean handlesRequestCode(int requestCode) {
        return requestCode > REQUEST_CODE_BASE && requestCode <= REQUEST_CODE_BASE + REQUEST_CODE_COUNT;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, UploadProxyMimeMode mode) {
        CallbackRequestRegistry.Request<Uri[]> request = currentRequest;
        if (request == null || requestCode != currentRequestCode) return;
        if (resultCode != Activity.RESULT_OK || data == null) {
            requests.complete(request, null);
            return;
        }
        List<Uri> selected = selectedUris(data);
        if (selected.isEmpty()) {
            requests.complete(request, null);
            return;
        }
        if (!allImages(selected)) {
            requests.complete(request, selected.toArray(new Uri[0]));
            return;
        }
        executor.execute(() -> createProxyResult(request, selected, mode));
    }

    private void createProxyResult(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected,
                                   UploadProxyMimeMode mode) {
        UploadProxyStore store = new UploadProxyStore(activity);
        OriginalMediaResolver originalResolver = new OriginalMediaResolver(activity);
        ArrayList<Uri> results = new ArrayList<>();
        int proxyCount = 0;
        boolean identical = true;
        String reportedMime = mode.reportedMime(null);
        OriginalMediaResolver.Status accessStatus = OriginalMediaResolver.Status.UNRESOLVED_PICKER_URI;
        Boolean gpsCoordinatesReadable = null;
        Boolean gpsAltitudeReadable = null;
        for (int i = 0; i < selected.size(); i++) {
            Uri source = selected.get(i);
            try {
                OriginalMediaResolver.Result original = originalResolver.resolve(source);
                accessStatus = original.status;
                UploadProxyStore.Result proxy = store.create(source, original.copySource(), i,
                        request.getGeneration(), mode);
                results.add(proxy.uri);
                proxyCount++;
                identical &= proxy.identical;
                reportedMime = proxy.reportedMime;
                gpsCoordinatesReadable = proxy.gpsCoordinatesReadable;
                gpsAltitudeReadable = proxy.gpsAltitudeReadable;
            } catch (IOException | RuntimeException e) {
                results.add(source);
                identical = false;
            }
        }
        UploadProxyDiagnostics diagnostics = new UploadProxyDiagnostics(mode, selected.size(), proxyCount,
                reportedMime, identical, true, accessStatus, gpsCoordinatesReadable, gpsAltitudeReadable);
        boolean hadError = proxyCount != selected.size();
        main.post(() -> complete(request, results.toArray(new Uri[0]), diagnostics, hadError));
    }

    private void complete(CallbackRequestRegistry.Request<Uri[]> request, Uri[] result,
                          UploadProxyDiagnostics diagnostics, boolean hadError) {
        if (!destroyed && requests.complete(request, result)) {
            delegate.onUploadProxyDiagnostics(diagnostics);
            if (hadError) Toast.makeText(activity, R.string.upload_proxy_error, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allImages(List<Uri> selected) {
        for (Uri uri : selected) {
            String mime = null;
            try { mime = activity.getContentResolver().getType(uri); }
            catch (RuntimeException ignored) { /* extension fallback below */ }
            if (!SelectedFileType.isImage(mime, uri.getLastPathSegment())) return false;
        }
        return true;
    }

    private static List<Uri> selectedUris(Intent intent) {
        ArrayList<Uri> result = new ArrayList<>();
        ClipData clip = intent.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) result.add(uri);
            }
        } else if (intent.getData() != null) {
            result.add(intent.getData());
        }
        return result;
    }

    public void destroy() {
        destroyed = true;
        requests.cancelActive();
        currentRequest = null;
        executor.shutdownNow();
    }
}
