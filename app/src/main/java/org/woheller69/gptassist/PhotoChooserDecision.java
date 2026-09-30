package org.woheller69.gptassist;

final class PhotoChooserDecision {
    private PhotoChooserDecision() {}
    static boolean useOpenDocument(boolean contextEnabled, boolean gpsEnabled, boolean acceptsImages,
                                   boolean captureEnabled, int apiLevel) {
        return contextEnabled && gpsEnabled && acceptsImages && !captureEnabled && apiLevel <= 36;
    }
}
