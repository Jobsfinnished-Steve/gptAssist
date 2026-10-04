package org.woheller69.gptassist;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

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
    private static final int LOCATION_PERMISSION_REQUEST = 8001;
    private static final int REQUEST_CODE_COUNT = 50000;
    private final Activity activity;
    private final Delegate delegate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CallbackRequestRegistry<Uri[]> requests = new CallbackRequestRegistry<>();
    private CallbackRequestRegistry.Request<Uri[]> currentRequest;
    private int currentRequestCode;
    private volatile boolean destroyed;
    private final PendingUploadPermission<PendingSelection> permission = new PendingUploadPermission<>();

    public PhotoUploadCoordinator(Activity activity, Delegate delegate) {
        this.activity = activity;
        this.delegate = delegate;
    }

    public boolean show(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        permission.clearSelection();
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
        // GET_CONTENT may be redirected to Photo Picker, whose URI can expose
        // redacted bytes. DocumentsUI gives a per-file grant usable by getMediaUri.
        if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        PendingSelection selection = new PendingSelection(request, selected, mode);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (permission.await(selection)) {
                try {
                    // Request only after selection; never block opening the file chooser.
                    // The document URI supplies read access without broad gallery access.
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION},
                            LOCATION_PERMISSION_REQUEST);
                } catch (RuntimeException e) {
                    resume(permission.finish());
                }
            }
            return;
        }
        resume(selection);
    }

    public void onRequestPermissionsResult(int requestCode) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) resume(permission.finish());
    }

    private void resume(PendingSelection selection) {
        if (destroyed || selection == null || selection.request != currentRequest) return;
        // Recheck actual permissions in OriginalMediaResolver, including denial/cancellation.
        executor.execute(() -> createProxyResult(selection.request, selection.selected, selection.mode));
    }

    private void createProxyResult(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected,
                                   UploadProxyMimeMode mode) {
        UploadProxyStore store = new UploadProxyStore(activity);
        OriginalMediaResolver originalResolver = new OriginalMediaResolver(activity);
        ArrayList<Uri> results = new ArrayList<>();
        int proxyCount = 0;
        boolean identical = true;
        String reportedMime = mode.reportedMime(null);
        ArrayList<UploadProxyDiagnostics.Item> items = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            Uri source = selected.get(i);
            OriginalMediaResolver.Status accessStatus = OriginalMediaResolver.Status.UNRESOLVED_PICKER_URI;
            try {
                OriginalMediaResolver.Result original = originalResolver.resolve(source);
                accessStatus = original.status;
                UploadProxyStore.Result proxy = store.create(source, original.copySource(), i,
                        request.getGeneration(), mode);
                results.add(proxy.uri);
                proxyCount++;
                identical &= proxy.identical;
                reportedMime = proxy.reportedMime;
                items.add(new UploadProxyDiagnostics.Item(i + 1, accessStatus,
                        proxy.identical, proxy.gpsCoordinatesReadable, proxy.gpsAltitudeReadable));
            } catch (IOException | RuntimeException e) {
                results.add(source);
                identical = false;
                items.add(new UploadProxyDiagnostics.Item(i + 1, accessStatus, false, null, null));
            }
        }
        UploadProxyDiagnostics diagnostics = new UploadProxyDiagnostics(mode, selected.size(), proxyCount,
                reportedMime, identical, true, items);
        boolean hadError = proxyCount != selected.size();
        main.post(() -> complete(request, results.toArray(new Uri[0]), diagnostics, hadError));
    }

    private void complete(CallbackRequestRegistry.Request<Uri[]> request, Uri[] result,
                          UploadProxyDiagnostics diagnostics, boolean hadError) {
        if (!destroyed && requests.complete(request, result)) {
            delegate.onUploadProxyDiagnostics(diagnostics);
            if (hadError) Toast.makeText(activity, R.string.upload_proxy_error, Toast.LENGTH_SHORT).show();
            else if (diagnostics.hasMissingGps()) {
                Toast.makeText(activity, R.string.upload_proxy_gps_missing, Toast.LENGTH_LONG).show();
            }
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
        permission.clearSelection();
        requests.cancelActive();
        currentRequest = null;
        executor.shutdownNow();
    }

    private static final class PendingSelection {
        final CallbackRequestRegistry.Request<Uri[]> request;
        final List<Uri> selected;
        final UploadProxyMimeMode mode;
        PendingSelection(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected,
                         UploadProxyMimeMode mode) {
            this.request = request;
            this.selected = new ArrayList<>(selected);
            this.mode = mode;
        }
    }
}
