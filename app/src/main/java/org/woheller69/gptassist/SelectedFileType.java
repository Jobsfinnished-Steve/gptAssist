package org.woheller69.gptassist;

import java.util.Locale;

/** Pure MIME/extension classification shared by the Android coordinator and JVM tests. */
public final class SelectedFileType {
    private SelectedFileType() {}

    public static boolean isImage(String resolverMimeType, String fileName) {
        if (resolverMimeType != null && !resolverMimeType.trim().isEmpty()) {
            return resolverMimeType.toLowerCase(Locale.US).startsWith("image/");
        }
        if (fileName == null) return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return false;
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.US);
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png")
                || extension.equals("gif") || extension.equals("webp") || extension.equals("bmp")
                || extension.equals("heic") || extension.equals("heif") || extension.equals("avif")
                || extension.equals("dng");
    }
}
