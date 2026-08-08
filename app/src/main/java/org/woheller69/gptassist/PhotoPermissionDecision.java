package org.woheller69.gptassist;

final class PhotoPermissionDecision {
    private PhotoPermissionDecision() {}

    static boolean shouldRequest(boolean contextEnabled, boolean gpsEnabled,
                                 boolean allSelectedAreImages, boolean androidQOrLater,
                                 boolean permissionGranted) {
        return contextEnabled && gpsEnabled && allSelectedAreImages && androidQOrLater && !permissionGranted;
    }
}
