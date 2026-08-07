package org.woheller69.gptassist;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

final class MediaUriResolver {
    private static final String MEDIA_DOCUMENTS = "com.android.providers.media.documents";

    private MediaUriResolver() {}

    static Uri toMediaStoreImage(Context context, Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return uri;
        if (MEDIA_DOCUMENTS.equals(uri.getAuthority()) && DocumentsContract.isDocumentUri(context, uri)) {
            String id;
            try { id = mediaImageId(DocumentsContract.getDocumentId(uri)); }
            catch (RuntimeException ignored) { return uri; }
            if (id != null) {
                try { return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id)); }
                catch (NumberFormatException ignored) { return uri; }
            }
        }
        return uri;
    }

    static boolean isMediaDocumentsAuthority(String authority) {
        return MEDIA_DOCUMENTS.equals(authority);
    }

    static boolean supportsRequireOriginal(Uri uri) {
        return uri != null && supportsRequireOriginal(uri.getScheme(), uri.getAuthority());
    }

    static boolean supportsRequireOriginal(String scheme, String authority) {
        return "content".equals(scheme)
                && ("media".equals(authority)
                || (authority != null && authority.startsWith("com.android.providers.media")));
    }

    static String mediaImageId(String documentId) {
        if (documentId == null) return null;
        int separator = documentId.indexOf(':');
        if (separator <= 0 || !"image".equals(documentId.substring(0, separator))) return null;
        String id = documentId.substring(separator + 1);
        return id.isEmpty() ? null : id;
    }
}
