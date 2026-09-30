package org.woheller69.gptassist;

final class UploadProxyDiagnostics {
    final UploadProxyMimeMode mode;
    final int selectedCount;
    final int proxyCount;
    final String reportedMime;
    final boolean bytesIdentical;
    final boolean callbackCompleted;
    final OriginalMediaResolver.Status originalAccessStatus;
    final Boolean gpsCoordinatesReadable;
    final Boolean gpsAltitudeReadable;
    UploadProxyDiagnostics(UploadProxyMimeMode mode, int selectedCount, int proxyCount,
                           String reportedMime, boolean bytesIdentical, boolean callbackCompleted,
                           OriginalMediaResolver.Status originalAccessStatus,
                           Boolean gpsCoordinatesReadable, Boolean gpsAltitudeReadable) {
        this.mode = mode; this.selectedCount = selectedCount; this.proxyCount = proxyCount;
        this.reportedMime = reportedMime; this.bytesIdentical = bytesIdentical;
        this.callbackCompleted = callbackCompleted; this.originalAccessStatus = originalAccessStatus;
        this.gpsCoordinatesReadable = gpsCoordinatesReadable;
        this.gpsAltitudeReadable = gpsAltitudeReadable;
    }
}
