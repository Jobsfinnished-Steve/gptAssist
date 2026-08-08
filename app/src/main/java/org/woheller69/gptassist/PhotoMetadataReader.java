package org.woheller69.gptassist;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        return read(uri, includeGps, false);
    }

    public PhotoContext read(Uri uri, boolean includeGps, boolean gpsPermissionDenied) {
        Uri mediaUri = MediaUriResolver.toMediaStoreImage(context, uri);
        OriginalAccess access = openExif(uri, mediaUri, includeGps);
        ExifInterface exif = access.exif;
        String captured = null;
        String offset = "unknown";
        PhotoContext.TimestampSource source = PhotoContext.TimestampSource.UNKNOWN;
        Double latitude = null;
        Double longitude = null;
        Double altitude = null;
        boolean cameraExif = false;
        PhotoContext.GpsReadStatus gpsStatus = GpsAccessOutcome.status(includeGps,
                gpsPermissionDenied, access.originalMetadataAvailable, access.exif != null);

        if (exif != null) {
            captured = ExifDateParser.parse(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL));
            if (captured != null) {
                source = PhotoContext.TimestampSource.EXIF_DATETIME_ORIGINAL;
                offset = ExifDateParser.normalizeOffset(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL));
            }
            cameraExif = exif.getAttribute(ExifInterface.TAG_MAKE) != null
                    || exif.getAttribute(ExifInterface.TAG_MODEL) != null;
            if (includeGps && access.originalMetadataAvailable) {
                try {
                    float[] location = new float[2];
                    if (exif.getLatLong(location)) {
                        latitude = (double) location[0];
                        longitude = (double) location[1];
                    }
                    boolean hasAltitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null;
                    boolean hasAltitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null;
                    altitude = GpsAltitudeValue.validated(hasAltitude, hasAltitudeRef,
                            exif.getAltitude(Double.NaN));
                    if (latitude == null || longitude == null) altitude = null;
                    gpsStatus = latitude != null && longitude != null
                            ? PhotoContext.GpsReadStatus.AVAILABLE : PhotoContext.GpsReadStatus.NO_GPS_TAG;
                } catch (RuntimeException ignored) {
                    latitude = null;
                    longitude = null;
                    altitude = null;
                    gpsStatus = PhotoContext.GpsReadStatus.READ_ERROR;
                }
            }
        }

        Row row = query(mediaUri);
        if (row.empty) row = query(uri);
        if (captured == null && validEpoch(row.dateTaken)) {
            captured = epoch(row.dateTaken);
            source = PhotoContext.TimestampSource.MEDIASTORE_DATE_TAKEN;
        }
        if (captured == null && validEpoch(row.lastModified)) {
            captured = epoch(row.lastModified);
            source = PhotoContext.TimestampSource.FILE_LAST_MODIFIED;
        }
        return new PhotoContext(captured, offset, source, latitude, longitude, altitude,
                classify(row.path, cameraExif), gpsStatus);
    }

    private OriginalAccess openExif(Uri selectedUri, Uri mediaUri, boolean includeGps) {
        if (!includeGps) {
            ExifInterface ordinary = openDirect(selectedUri, "DIRECT_URI");
            return new OriginalAccess(ordinary, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Bundle options = new Bundle();
            options.putBoolean(MediaStore.EXTRA_ACCEPT_ORIGINAL_MEDIA_FORMAT, true);
            try (AssetFileDescriptor descriptor = resolver.openTypedAssetFileDescriptor(
                    selectedUri, "image/*", options)) {
                if (descriptor != null) {
                    ExifInterface exif = new ExifInterface(descriptor.getFileDescriptor());
                    diagnostic("OPEN_TYPED_ORIGINAL", "SUCCESS");
                    return new OriginalAccess(exif, true);
                }
                diagnostic("OPEN_TYPED_ORIGINAL", "NO_DESCRIPTOR");
            } catch (SecurityException e) {
                diagnostic("OPEN_TYPED_ORIGINAL", "SECURITY_EXCEPTION");
            } catch (IOException | RuntimeException e) {
                diagnostic("OPEN_TYPED_ORIGINAL", "IO_ERROR");
            }
        } else {
            diagnostic("OPEN_TYPED_ORIGINAL", "UNSUPPORTED");
        }

        ExifInterface ordinary = openDirect(selectedUri, "DIRECT_URI");
        boolean selectedDocument = "content".equals(selectedUri.getScheme())
                && DocumentsContract.isDocumentUri(context, selectedUri);
        boolean mediaDocument = MediaUriResolver.isMediaDocumentsAuthority(selectedUri.getAuthority());
        if (ordinary != null && selectedDocument && !mediaDocument) {
            return new OriginalAccess(ordinary, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaUri != null
                && !mediaUri.equals(selectedUri) && MediaUriResolver.supportsRequireOriginal(mediaUri)) {
            try {
                Uri original = MediaStore.setRequireOriginal(mediaUri);
                try (InputStream input = resolver.openInputStream(original)) {
                    if (input != null) {
                        ExifInterface exif = new ExifInterface(input);
                        diagnostic("MEDIASTORE_REQUIRE_ORIGINAL", "SUCCESS");
                        return new OriginalAccess(exif, true);
                    }
                    diagnostic("MEDIASTORE_REQUIRE_ORIGINAL", "NO_DESCRIPTOR");
                }
            } catch (SecurityException e) {
                diagnostic("MEDIASTORE_REQUIRE_ORIGINAL", "SECURITY_EXCEPTION");
            } catch (IOException | RuntimeException e) {
                diagnostic("MEDIASTORE_REQUIRE_ORIGINAL", "IO_ERROR");
            }
        } else {
            diagnostic("MEDIASTORE_REQUIRE_ORIGINAL", "UNSUPPORTED");
        }

        return new OriginalAccess(ordinary, false);
    }

    private ExifInterface openDirect(Uri uri, String attempt) {
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                diagnostic(attempt, "NO_DESCRIPTOR");
                return null;
            }
            ExifInterface exif = new ExifInterface(input);
            diagnostic(attempt, "SUCCESS");
            return exif;
        } catch (SecurityException e) {
            diagnostic(attempt, "SECURITY_EXCEPTION");
        } catch (IOException | RuntimeException e) {
            diagnostic(attempt, "IO_ERROR");
        }
        return null;
    }

    private static void diagnostic(String attempt, String result) {
        if (BuildConfig.DEBUG) Log.d("PhotoMetadataReader", attempt + " -> " + result);
    }

    private Row query(Uri uri) {
        String[] columns = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME}
                : new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        Row row = new Row();
        try (Cursor cursor = resolver.query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                row.empty = false;
                row.dateTaken = number(cursor, MediaStore.Images.ImageColumns.DATE_TAKEN, false);
                row.lastModified = number(cursor, MediaStore.MediaColumns.DATE_MODIFIED, true);
                String relativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? string(cursor, MediaStore.MediaColumns.RELATIVE_PATH) : "";
                row.path = relativePath + "/" + string(cursor, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
            }
        } catch (RuntimeException ignored) { /* provider may reject MediaStore columns */ }
        if (row.lastModified == 0) {
            try (Cursor cursor = resolver.query(uri, new String[]{DocumentsContract.Document.COLUMN_LAST_MODIFIED}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    row.empty = false;
                    row.lastModified = number(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED, false);
                }
            } catch (RuntimeException ignored) { /* not a DocumentsProvider */ }
        }
        if (row.lastModified == 0 && "file".equals(uri.getScheme()) && uri.getPath() != null) {
            row.empty = false;
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
        if (cameraExif && (lower.contains("dcim/camera") || lower.contains("/camera") || lower.equals("/camera"))) {
            return PhotoContext.MediaType.CAMERA_PHOTO;
        }
        return PhotoContext.MediaType.UNKNOWN;
    }

    private static final class OriginalAccess {
        final ExifInterface exif;
        final boolean originalMetadataAvailable;
        OriginalAccess(ExifInterface exif, boolean originalMetadataAvailable) {
            this.exif = exif;
            this.originalMetadataAvailable = originalMetadataAvailable;
        }
    }
    private static final class Row { long dateTaken; long lastModified; String path = ""; boolean empty = true; }
}
