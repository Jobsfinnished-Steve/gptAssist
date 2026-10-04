package org.woheller69.gptassist;

final class GpsValueValidation {
    private GpsValueValidation() {}

    static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    static boolean coordinates(double[] values) {
        return values != null && values.length == 2
                && finite(values[0]) && finite(values[1])
                && Math.abs(values[0]) <= 90 && Math.abs(values[1]) <= 180;
    }
}
