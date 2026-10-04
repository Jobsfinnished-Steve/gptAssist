package org.woheller69.gptassist;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

final class OriginalMediaResolver {
    enum Status {
        ORIGINAL_AVAILABLE,
        DIRECT_MEDIASTORE_URI,
        DOCUMENT_MEDIASTORE_RESOLVED,
        UNRESOLVED_PICKER_URI,
        MEDIA_PERMISSION_DENIED,
        LOCATION_PERMISSION_DENIED,
        LIMITED_MEDIA_ACCESS,
        REQUIRE_ORIGINAL_SECURITY_EXCEPTION,
        REQUIRE_ORIGINAL_IO_ERROR
    }
    enum MediaAccess { FULL, LIMITED, DENIED }

    static final class Result {
        final Uri selectedUri;
        final Uri mediaStoreUri;
        final Uri originalUri;
        final Status status;
        Result(Uri selectedUri, Uri mediaStoreUri, Uri originalUri, Status status) {
            this.selectedUri = selectedUri;
            this.mediaStoreUri = mediaStoreUri;
            this.originalUri = originalUri;
            this.status = status;
        }
        Uri copySource() { return originalUri == null ? selectedUri : originalUri; }
    }

    private final Context context;
    private final ContentResolver resolver;

    OriginalMediaResolver(Context context) {
        this.context = context.getApplicationContext();
        resolver = context.getContentResolver();
    }

    Result resolve(Uri selectedUri) {
        MediaAccess mediaAccess = mediaAccess(context);
        // A document URI grant is sufficient to try the selected file. Broad
        // READ_MEDIA_IMAGES denial must not discard that per-file capability.
        Uri mediaStoreUri = canonicalMediaStoreUri(selectedUri);
        if (mediaStoreUri == null) {
            return new Result(selectedUri, null, null, mediaAccess == MediaAccess.LIMITED
                    ? Status.LIMITED_MEDIA_ACCESS : Status.UNRESOLVED_PICKER_URI);
        }
        Status resolvedStatus = mediaStoreUri.equals(selectedUri)
                ? Status.DIRECT_MEDIASTORE_URI : Status.DOCUMENT_MEDIASTORE_RESOLVED;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return readable(selectedUri, mediaStoreUri, mediaStoreUri, resolvedStatus);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_MEDIA_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return new Result(selectedUri, mediaStoreUri, null, Status.LOCATION_PERMISSION_DENIED);
        }
        try {
            Uri original = MediaStore.setRequireOriginal(mediaStoreUri);
            return readable(selectedUri, mediaStoreUri, original, Status.ORIGINAL_AVAILABLE);
        } catch (SecurityException e) {
            return new Result(selectedUri, mediaStoreUri, null, Status.REQUIRE_ORIGINAL_SECURITY_EXCEPTION);
        } catch (RuntimeException e) {
            return new Result(selectedUri, mediaStoreUri, null, Status.REQUIRE_ORIGINAL_IO_ERROR);
        }
    }

    private Result readable(Uri selected, Uri media, Uri original, Status success) {
        try (InputStream input = resolver.openInputStream(original)) {
            if (input == null) return new Result(selected, media, null, Status.REQUIRE_ORIGINAL_IO_ERROR);
            return new Result(selected, media, original, success);
        } catch (SecurityException e) {
            return new Result(selected, media, null, Status.REQUIRE_ORIGINAL_SECURITY_EXCEPTION);
        } catch (IOException | RuntimeException e) {
            return new Result(selected, media, null, Status.REQUIRE_ORIGINAL_IO_ERROR);
        }
    }

    Uri canonicalMediaStoreUri(Uri uri) {
        if (isDirectMediaStoreImage(uri)) return uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // This preserves the document grant, supports MediaDocumentsProvider
                // and ExternalStorageProvider, and does not guess IDs from paths.
                Uri mediaUri = MediaStore.getMediaUri(context, uri);
                if (isDirectMediaStoreImage(mediaUri)) return mediaUri;
            } catch (RuntimeException ignored) { /* Unsupported or unavailable provider. */ }
            return null;
        }
        if (!"com.android.providers.media.documents".equals(uri.getAuthority())
                || !DocumentsContract.isDocumentUri(context, uri)) return null;
        String documentId;
        try { documentId = DocumentsContract.getDocumentId(uri); }
        catch (RuntimeException e) { return null; }
        Long id = numericImageId(documentId);
        return id == null ? null : ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
    }

    static Long numericImageId(String documentId) {
        if (documentId == null || !documentId.matches("image:[0-9]+")) return null;
        try { return Long.parseLong(documentId.substring("image:".length())); }
        catch (NumberFormatException e) { return null; }
    }

    static boolean isDirectMediaStoreImage(Uri uri) {
        if (uri == null || !ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
                || !"media".equals(uri.getAuthority())) return false;
        return isDirectMediaStoreImagePath(uri.getPathSegments());
    }


    static boolean isDirectMediaStoreImagePath(java.util.List<String> parts) {
        if (parts == null) return false;
        boolean images = parts.size() == 4 && "images".equals(parts.get(1))
                && "media".equals(parts.get(2));
        // getMediaUri can map an ExternalStorageProvider document to Files.
        // Callers have already classified the selected item as an image.
        boolean files = parts.size() == 3 && "file".equals(parts.get(1));
        if (!images && !files) return false;
        String volume = parts.get(0);
        if (!("external".equals(volume) || "external_primary".equals(volume)
                || "internal".equals(volume) || volume.matches("[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"))) return false;
        try { return Long.parseLong(parts.get(parts.size() - 1)) >= 0; }
        catch (NumberFormatException e) { return false; }
    }

    static MediaAccess classifyMediaAccess(boolean full, boolean limited) {
        if (full) return MediaAccess.FULL;
        return limited ? MediaAccess.LIMITED : MediaAccess.DENIED;
    }
    static MediaAccess mediaAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean full = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
            boolean limited = Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
            return classifyMediaAccess(full, limited);
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED ? MediaAccess.FULL : MediaAccess.DENIED;
    }
}
