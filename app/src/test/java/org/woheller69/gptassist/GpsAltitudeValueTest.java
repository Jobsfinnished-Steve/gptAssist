package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class GpsAltitudeValueTest {
    @Test public void acceptsAboveAndBelowSeaLevelWithoutRenegating() {
        assertEquals(42.3, GpsAltitudeValue.validated(true, true, 42.3), 0.0);
        assertEquals(-12.5, GpsAltitudeValue.validated(true, true, -12.5), 0.0);
    }

    @Test public void missingAltitudeOrReferenceIsAbsent() {
        assertNull(GpsAltitudeValue.validated(false, true, 42.3));
        assertNull(GpsAltitudeValue.validated(true, false, 42.3));
    }

    @Test public void malformedAltitudeIsAbsent() {
        assertNull(GpsAltitudeValue.validated(true, true, Double.NaN));
        assertNull(GpsAltitudeValue.validated(true, true, Double.POSITIVE_INFINITY));
    }
}
