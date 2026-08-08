package org.woheller69.gptassist;

final class UploadProxyDiagnostics {
    final UploadProxyMimeMode mode;
    final int selectedCount;
    final int proxyCount;
    final String reportedMime;
    final boolean bytesIdentical;
    final boolean callbackCompleted;
    UploadProxyDiagnostics(UploadProxyMimeMode mode, int selectedCount, int proxyCount,
                           String reportedMime, boolean bytesIdentical, boolean callbackCompleted) {
        this.mode = mode; this.selectedCount = selectedCount; this.proxyCount = proxyCount;
        this.reportedMime = reportedMime; this.bytesIdentical = bytesIdentical;
        this.callbackCompleted = callbackCompleted;
    }
}
