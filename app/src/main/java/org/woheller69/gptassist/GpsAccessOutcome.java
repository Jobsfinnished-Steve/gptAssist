package org.woheller69.gptassist;

final class GpsAccessOutcome {
    private GpsAccessOutcome() {}
    static PhotoContext.GpsReadStatus status(boolean gpsEnabled, boolean permissionDenied,
                                             boolean originalOpened, boolean ordinaryOpened) {
        if (permissionDenied) return PhotoContext.GpsReadStatus.PERMISSION_DENIED;
        if (!gpsEnabled) return PhotoContext.GpsReadStatus.GPS_DISABLED;
        if (originalOpened) return PhotoContext.GpsReadStatus.NO_GPS_TAG;
        if (ordinaryOpened) return PhotoContext.GpsReadStatus.ORIGINAL_ACCESS_FAILED;
        return PhotoContext.GpsReadStatus.READ_ERROR;
    }
}
