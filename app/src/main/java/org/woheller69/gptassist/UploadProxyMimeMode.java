package org.woheller69.gptassist;

public enum UploadProxyMimeMode {
    TEXT("text/plain"), OCTET_STREAM("application/octet-stream"), IMAGE_MIME(null);
    private final String fixedMime;
    UploadProxyMimeMode(String fixedMime) { this.fixedMime = fixedMime; }
    String reportedMime(String sourceMime) {
        if (fixedMime != null) return fixedMime;
        return sourceMime != null && sourceMime.startsWith("image/") ? sourceMime : "image/jpeg";
    }
    static UploadProxyMimeMode fromPreference(String value) {
        try { return valueOf(value); } catch (RuntimeException ignored) { return TEXT; }
    }
    UploadProxyMimeMode next() { return values()[(ordinal() + 1) % values().length]; }
}
