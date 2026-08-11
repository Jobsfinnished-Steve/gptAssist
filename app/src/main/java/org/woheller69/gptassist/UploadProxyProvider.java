package org.woheller69.gptassist;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class UploadProxyProvider extends ContentProvider {
    static final String PATH = "proxy";

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        return UploadProxyMimeMode.fromPreference(uri.getQueryParameter("mode"))
                .reportedMime(uri.getQueryParameter("source_mime"));
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        File file = resolve(uri);
        if (file == null) return null;
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add(file.getName());
            else if (OpenableColumns.SIZE.equals(column)) row.add(file.length());
            else row.add(null);
        }
        return cursor;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only provider");
        File file = resolve(uri);
        if (file == null || !file.isFile()) throw new FileNotFoundException("Proxy unavailable");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File resolve(Uri uri) {
        if (getContext() == null || uri.getPathSegments().size() != 3
                || !PATH.equals(uri.getPathSegments().get(0))) return null;
        File root = new File(getContext().getCacheDir(), UploadProxyStore.DIRECTORY);
        File file = new File(new File(root, uri.getPathSegments().get(1)), uri.getPathSegments().get(2));
        try {
            String rootPath = root.getCanonicalPath() + File.separator;
            return file.getCanonicalPath().startsWith(rootPath) ? file : null;
        } catch (IOException ignored) { return null; }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
