package org.woheller69.gptassist;

final class GpsAltitudeValue {
    private GpsAltitudeValue() {}

    static Double validated(boolean hasAltitudeTag, boolean hasAltitudeReference, double altitudeMeters) {
        if (!hasAltitudeTag || !hasAltitudeReference
                || Double.isNaN(altitudeMeters) || Double.isInfinite(altitudeMeters)) return null;
        return altitudeMeters;
    }
}
