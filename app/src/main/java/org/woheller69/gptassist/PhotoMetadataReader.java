package org.woheller69.gptassist;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PhotoMetadataReader {
    private final Context context;
    private final ContentResolver resolver;

    public PhotoMetadataReader(Context context) {
        this.context = context.getApplicationContext();
        resolver = context.getContentResolver();
    }

    public PhotoContext read(Uri uri, boolean includeGps) {
        Uri metadataUri = originalUri(uri, includeGps);
        ExifInterface exif = null;
        try (InputStream input = resolver.openInputStream(metadataUri)) {
            if (input != null) exif = new ExifInterface(input);
        } catch (IOException | RuntimeException ignored) {
            if (!metadataUri.equals(uri)) {
                try (InputStream input = resolver.openInputStream(uri)) {
                    if (input != null) exif = new ExifInterface(input);
                } catch (IOException | RuntimeException ignoredAgain) { /* fallbacks below */ }
            }
        }

        String captured = null;
        String offset = "unknown";
        PhotoContext.TimestampSource source = PhotoContext.TimestampSource.UNKNOWN;
        Double latitude = null;
        Double longitude = null;
        boolean cameraExif = false;
        if (exif != null) {
            captured = ExifDateParser.parse(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL));
            if (captured != null) {
                source = PhotoContext.TimestampSource.EXIF_DATETIME_ORIGINAL;
                offset = ExifDateParser.normalizeOffset(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL));
            }
            cameraExif = exif.getAttribute(ExifInterface.TAG_MAKE) != null || exif.getAttribute(ExifInterface.TAG_MODEL) != null;
            if (includeGps) {
                float[] location = new float[2];
                try {
                    if (exif.getLatLong(location)) {
                        latitude = (double) location[0];
                        longitude = (double) location[1];
                    }
                } catch (RuntimeException ignored) { /* GPS remains absent */ }
            }
        }

        Row row = query(uri);
        if (captured == null && validEpoch(row.dateTaken)) {
            captured = epoch(row.dateTaken);
            source = PhotoContext.TimestampSource.MEDIASTORE_DATE_TAKEN;
        }
        if (captured == null && validEpoch(row.lastModified)) {
            captured = epoch(row.lastModified);
            source = PhotoContext.TimestampSource.FILE_LAST_MODIFIED;
        }
        return new PhotoContext(captured, offset, source, latitude, longitude, classify(row.path, cameraExif));
    }

    private Uri originalUri(Uri uri, boolean includeGps) {
        if (includeGps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "content".equals(uri.getScheme())) {
            try { return MediaStore.setRequireOriginal(uri); } catch (RuntimeException ignored) { return uri; }
        }
        return uri;
    }

    private Row query(Uri uri) {
        String[] columns = {MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        Row row = new Row();
        try (Cursor cursor = resolver.query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                row.dateTaken = number(cursor, MediaStore.Images.ImageColumns.DATE_TAKEN, false);
                row.lastModified = number(cursor, MediaStore.MediaColumns.DATE_MODIFIED, true);
                row.path = string(cursor, MediaStore.MediaColumns.RELATIVE_PATH) + "/" +
                        string(cursor, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
            }
        } catch (RuntimeException ignored) { /* provider may reject unsupported columns */ }
        if (row.lastModified == 0) {
            try (Cursor cursor = resolver.query(uri, new String[]{DocumentsContract.Document.COLUMN_LAST_MODIFIED}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) row.lastModified = number(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED, false);
            } catch (RuntimeException ignored) { /* not a DocumentsProvider */ }
        }
        if (row.lastModified == 0 && "file".equals(uri.getScheme()) && uri.getPath() != null) {
            row.lastModified = new File(uri.getPath()).lastModified();
        }
        return row;
    }

    private static long number(Cursor c, String name, boolean seconds) {
        int index = c.getColumnIndex(name);
        if (index < 0 || c.isNull(index)) return 0;
        long result = c.getLong(index);
        return seconds && result > 0 ? result * 1000L : result;
    }
    private static String string(Cursor c, String name) {
        int index = c.getColumnIndex(name);
        return index < 0 || c.isNull(index) ? "" : c.getString(index);
    }
    private static boolean validEpoch(long value) { return value >= 315532800000L && value <= System.currentTimeMillis() + 86400000L; }
    private static String epoch(long value) { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date(value)); }
    private static PhotoContext.MediaType classify(String path, boolean cameraExif) {
        String lower = path == null ? "" : path.toLowerCase(Locale.US);
        if (lower.contains("screenshot")) return PhotoContext.MediaType.SCREENSHOT;
        if (lower.contains("download")) return PhotoContext.MediaType.DOWNLOADED;
        if (cameraExif && (lower.contains("dcim/camera") || lower.contains("/camera"))) return PhotoContext.MediaType.CAMERA_PHOTO;
        return PhotoContext.MediaType.UNKNOWN;
    }
    private static final class Row { long dateTaken; long lastModified; String path = ""; }
}
