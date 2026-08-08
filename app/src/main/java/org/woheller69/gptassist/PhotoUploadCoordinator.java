package org.woheller69.gptassist;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PhotoUploadCoordinator {
    public interface Delegate {
        boolean isMediaLocationPermissionGranted();
        void requestMediaLocationPermission(long requestGeneration);
        void onPhotoContextReady(long requestGeneration, String context);
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
    private final PendingPermissionRegistry<PendingSelection> pendingPermissions = new PendingPermissionRegistry<>();
    private volatile boolean destroyed;
    private String lastContext;

    public PhotoUploadCoordinator(Activity activity, Delegate delegate) {
        this.activity = activity;
        this.delegate = delegate;
    }

    public boolean show(ValueCallback<Uri[]> next, WebChromeClient.FileChooserParams params, boolean contextEnabled, boolean includeGps) {
        pendingPermissions.clear();
        currentRequest = requests.begin(next::onReceiveValue);
        currentRequestCode = REQUEST_CODE_BASE + 1
                + (int) ((currentRequest.getGeneration() - 1) % REQUEST_CODE_COUNT);
        boolean imageLibraryRequest = PhotoChooserDecision.useOpenDocument(contextEnabled, includeGps,
                acceptsImages(params.getAcceptTypes()), params.isCaptureEnabled(), Build.VERSION.SDK_INT);
        Intent intent;
        if (imageLibraryRequest) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
        } else {
            try { intent = params.createIntent(); }
            catch (RuntimeException e) { intent = new Intent(Intent.ACTION_GET_CONTENT); }
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (acceptsImages(params.getAcceptTypes())
                    && (intent.getType() == null || "*/*".equals(intent.getType()))) intent.setType("image/*");
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);
        try { activity.startActivityForResult(intent, currentRequestCode); }
        catch (RuntimeException e) {
            requests.complete(currentRequest, null);
            Toast.makeText(activity, R.string.photo_read_error, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public boolean handlesRequestCode(int requestCode) {
        return requestCode > REQUEST_CODE_BASE && requestCode <= REQUEST_CODE_BASE + REQUEST_CODE_COUNT;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, boolean contextEnabled,
                                 boolean includeGps, UploadProxyMimeMode proxyMode) {
        CallbackRequestRegistry.Request<Uri[]> request = currentRequest;
        if (request == null || requestCode != currentRequestCode) return;
        if (resultCode != Activity.RESULT_OK || data == null) { requests.complete(request, null); return; }
        List<Uri> selected = selectedUris(data);
        if (selected.isEmpty()) { requests.complete(request, null); return; }
        boolean allImages = allSelectedUrisAreImages(selected);
        if (!allImages) {
            requests.complete(request, selected.toArray(new Uri[0]));
            return;
        }
        executor.execute(() -> createProxyResult(request, selected, proxyMode));
    }

    private void createProxyResult(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected,
                                   UploadProxyMimeMode mode) {
        UploadProxyStore store = new UploadProxyStore(activity);
        ArrayList<Uri> results = new ArrayList<>();
        int proxyCount = 0;
        boolean identical = true;
        String reportedMime = mode.reportedMime(null);
        for (int i = 0; i < selected.size(); i++) {
            Uri source = selected.get(i);
            try {
                UploadProxyStore.Result proxy = store.create(source, i, request.getGeneration(), mode);
                results.add(proxy.uri);
                proxyCount++;
                identical &= proxy.identical;
                reportedMime = proxy.reportedMime;
            } catch (IOException | RuntimeException e) {
                results.add(source);
                identical = false;
            }
        }
        int finalProxyCount = proxyCount;
        boolean finalIdentical = identical;
        String finalReportedMime = reportedMime;
        main.post(() -> completeProxy(request, results.toArray(new Uri[0]),
                new UploadProxyDiagnostics(mode, selected.size(), finalProxyCount,
                        finalReportedMime, finalIdentical, true), finalProxyCount != selected.size()));
    }

    private void completeProxy(CallbackRequestRegistry.Request<Uri[]> request, Uri[] result,
                               UploadProxyDiagnostics diagnostics, boolean hadError) {
        if (!destroyed && requests.complete(request, result)) {
            delegate.onUploadProxyDiagnostics(diagnostics);
            if (hadError) Toast.makeText(activity, R.string.upload_proxy_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void onMediaLocationPermissionResult(long requestGeneration, boolean granted) {
        PendingPermissionRegistry.Pending<PendingSelection> retained = pendingPermissions.consume(requestGeneration);
        if (retained == null) return;
        PendingSelection pending = retained.value;
        if (currentRequest != pending.request) return;
        executor.execute(() -> createResult(pending.request, pending.selected, granted, !granted));
    }

    private void createResult(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected,
                              boolean includeGps, boolean gpsPermissionDenied) {
        PhotoMetadataReader reader = new PhotoMetadataReader(activity);
        List<PhotoContext> contexts = new ArrayList<>();
        boolean hadError = false;
        for (Uri uri : selected) {
            try { contexts.add(reader.read(uri, includeGps, gpsPermissionDenied)); }
            catch (RuntimeException e) { contexts.add(PhotoContext.unknown()); hadError = true; }
        }
        PhotoUploadDelivery<Uri> delivery = new PhotoUploadDelivery<>(selected,
                PhotoContextFormatter.format(contexts));
        boolean finalHadError = hadError;
        main.post(() -> complete(request, delivery.attachments.toArray(new Uri[0]),
                delivery.context, finalHadError));
    }

    private void complete(CallbackRequestRegistry.Request<Uri[]> request, Uri[] result, String text, boolean hadError) {
        if (!destroyed && requests.complete(request, result)) {
            lastContext = text;
            delegate.onPhotoContextReady(request.getGeneration(), text);
            if (hadError) Toast.makeText(activity, R.string.photo_partial_metadata_error, Toast.LENGTH_SHORT).show();
        }
    }

    public String getLastContext() { return lastContext; }
    public void destroy() {
        destroyed = true;
        pendingPermissions.clear();
        requests.cancelActive();
        currentRequest = null;
        executor.shutdownNow();
    }

    private boolean allSelectedUrisAreImages(List<Uri> selected) {
        for (Uri uri : selected) {
            String mimeType = null;
            try { mimeType = activity.getContentResolver().getType(uri); }
            catch (RuntimeException ignored) { /* extension fallback below */ }
            if (!SelectedFileType.isImage(mimeType, uri.getLastPathSegment())) return false;
        }
        return true;
    }
    private static List<Uri> selectedUris(Intent intent) {
        ArrayList<Uri> result = new ArrayList<>();
        ClipData clip = intent.getClipData();
        if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) {
            Uri uri = clip.getItemAt(i).getUri();
            if (uri != null) result.add(uri);
        } else if (intent.getData() != null) result.add(intent.getData());
        return result;
    }
    private static boolean acceptsImages(String[] types) {
        if (types == null || types.length == 0) return false;
        for (String type : types) if (type != null && type.startsWith("image/")) return true;
        return false;
    }

    private static final class PendingSelection {
        final CallbackRequestRegistry.Request<Uri[]> request;
        final List<Uri> selected;
        PendingSelection(CallbackRequestRegistry.Request<Uri[]> request, List<Uri> selected) {
            this.request = request;
            this.selected = new ArrayList<>(selected);
        }
    }
}
