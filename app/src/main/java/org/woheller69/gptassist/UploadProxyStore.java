package org.woheller69.gptassist;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

final class UploadProxyStore {
    static final String DIRECTORY = "chatgpt_upload_proxy";
    private static final long MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    private final Context context;
    private final ContentResolver resolver;
    private final String operationId = UUID.randomUUID().toString();

    UploadProxyStore(Context context) {
        this.context = context.getApplicationContext();
        this.resolver = context.getContentResolver();
    }

    Result create(Uri selectedSource, Uri copySource, int index, long generation, UploadProxyMimeMode mode) throws IOException {
        File root = new File(context.getCacheDir(), DIRECTORY);
        File operation = new File(root, operationId + "-" + generation);
        cleanup(root, operation, System.currentTimeMillis());
        if (!operation.exists() && !operation.mkdirs()) throw new IOException("Unable to create proxy cache");
        String sourceName = displayName(selectedSource);
        String sourceMime = resolver.getType(selectedSource);
        if (sourceMime == null) sourceMime = mimeForName(sourceName);
        String fallbackExtension = extensionForMime(sourceMime);
        String name = disguisedName(sourceName, index, fallbackExtension);
        // Distinct selected images can have the same display name.
        while (new File(operation, name).exists()) name = (index + 1) + "_" + name;
        File output = new File(operation, name);
        MessageDigest copiedDigest = sha256();
        try (InputStream input = resolver.openInputStream(copySource);
             FileOutputStream stream = new FileOutputStream(output, false)) {
            if (input == null) throw new IOException("Source unavailable");
            copy(input, stream, copiedDigest);
        }
        String reportedMime = mode.reportedMime(sourceMime);
        Uri proxy = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(context.getPackageName() + ".upload_proxy")
                .appendPath(UploadProxyProvider.PATH).appendPath(operation.getName()).appendPath(name)
                .appendQueryParameter("mode", mode.name())
                .appendQueryParameter("source_mime", sourceMime == null ? "" : sourceMime).build();
        verifyProviderMetadata(proxy, name, reportedMime);
        // Verify the exact provider stream handed to WebView, in release builds too.
        boolean identical;
        try (InputStream input = resolver.openInputStream(proxy)) {
            if (input == null) throw new IOException("Proxy unavailable");
            identical = MessageDigest.isEqual(copiedDigest.digest(), hash(input));
        }
        if (!identical) throw new IOException("Proxy bytes differ from copy source");
        Boolean[] gps;
        try (InputStream input = resolver.openInputStream(proxy)) {
            gps = readableGps(input);
        }
        Boolean gpsCoordinatesReadable = gps[0];
        Boolean gpsAltitudeReadable = gps[1];
        if (BuildConfig.DEBUG) {
            Log.d("UploadProxyStore", "UPLOAD_PROXY_BYTES_IDENTICAL=" + identical);
            Log.d("UploadProxyStore", "UPLOAD_PROXY_GPS_COORDINATES_READABLE=" + gps[0]);
            Log.d("UploadProxyStore", "UPLOAD_PROXY_GPS_ALTITUDE_READABLE=" + gps[1]);
        }
        return new Result(proxy, reportedMime, identical, gpsCoordinatesReadable, gpsAltitudeReadable);
    }

    void cleanup() { cleanup(new File(context.getCacheDir(), DIRECTORY), null, System.currentTimeMillis()); }

    static String disguisedName(String displayName, int index, String fallbackExtension) {
        String base = displayName == null || displayName.trim().isEmpty()
                ? String.format(Locale.US, "image_%03d%s", index + 1, fallbackExtension) : displayName;
        base = base.replaceAll("[^A-Za-z0-9._-]", "_").replace("..", "_");
        while (base.startsWith(".")) base = base.substring(1);
        if (base.isEmpty()) base = String.format(Locale.US, "image_%03d%s", index + 1, fallbackExtension);
        return base + ".txt";
    }

    static void cleanup(File root, File protectedDirectory, long now) {
        File[] children = root.listFiles();
        if (children == null) return;
        long cutoff = now - MAX_AGE_MS;
        for (File child : children) {
            if (protectedDirectory != null && child.equals(protectedDirectory)) continue;
            if (child.lastModified() < cutoff) deleteRecursively(child);
        }
    }

    private void verifyProviderMetadata(Uri proxy, String expectedName, String expectedMime) throws IOException {
        String actualName = null;
        try (Cursor cursor = resolver.query(proxy, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) actualName = cursor.getString(column);
            }
        } catch (RuntimeException e) { throw new IOException("Proxy metadata unavailable", e); }
        String actualMime = resolver.getType(proxy);
        if (!expectedName.equals(actualName) || !expectedMime.equals(actualMime)) {
            throw new IOException("Proxy metadata mismatch");
        }
    }

    static void copy(InputStream input, java.io.OutputStream output, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (digest != null) digest.update(buffer, 0, read);
        }
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0 && !cursor.isNull(column)) return cursor.getString(column);
            }
        } catch (RuntimeException ignored) { /* deterministic fallback */ }
        return null;
    }
    private static String mimeForName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase(Locale.US);
        if (lower.endsWith(".heic")) return "image/heic";
        if (lower.endsWith(".heif")) return "image/heif";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return null;
    }
    private static String extensionForMime(String mime) {
        if ("image/heic".equals(mime) || "image/heif".equals(mime)) return ".heic";
        if ("image/png".equals(mime)) return ".png";
        return ".jpg";
    }
    private static Boolean[] readableGps(InputStream input) {
        if (input == null) return new Boolean[]{null, null};
        try {
            ExifInterface exif = new ExifInterface(input);
            boolean hasCoordinates = GpsValueValidation.coordinates(exif.getLatLong());
            double altitude = exif.getAltitude(Double.NaN);
            boolean hasAltitude = GpsValueValidation.finite(altitude);
            return new Boolean[]{hasCoordinates, hasAltitude};
        } catch (IOException | RuntimeException ignored) {
            return new Boolean[]{null, null};
        }
    }

    private static MessageDigest sha256() throws IOException {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IOException(e); }
    }
    private static byte[] hash(InputStream input) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[64 * 1024]; int read;
        while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        return digest.digest();
    }
    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        file.delete();
    }

    static final class Result {
        final Uri uri; final String reportedMime; final boolean identical;
        final Boolean gpsCoordinatesReadable; final Boolean gpsAltitudeReadable;
        Result(Uri uri, String reportedMime, boolean identical,
               Boolean gpsCoordinatesReadable, Boolean gpsAltitudeReadable) {
            this.uri = uri; this.reportedMime = reportedMime; this.identical = identical;
            this.gpsCoordinatesReadable = gpsCoordinatesReadable;
            this.gpsAltitudeReadable = gpsAltitudeReadable;
        }
    }
}
