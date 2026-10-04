package org.woheller69.gptassist;

import org.junit.Test;
import static org.junit.Assert.*;

public class GpsValueValidationTest {
    @Test public void rejectsRedactedNonFiniteAndOutOfRangeCoordinates() {
        assertFalse(GpsValueValidation.coordinates(null));
        assertFalse(GpsValueValidation.coordinates(new double[]{Double.NaN, Double.NaN}));
        assertFalse(GpsValueValidation.coordinates(new double[]{0, Double.POSITIVE_INFINITY}));
        assertFalse(GpsValueValidation.coordinates(new double[]{91, 0}));
        assertFalse(GpsValueValidation.coordinates(new double[]{0, -181}));
        assertFalse(GpsValueValidation.coordinates(new double[]{0}));
    }

    @Test public void acceptsEquatorPrimeMeridianAndBoundaryCoordinates() {
        assertTrue(GpsValueValidation.coordinates(new double[]{0, 0}));
        assertTrue(GpsValueValidation.coordinates(new double[]{-90, 180}));
        assertTrue(GpsValueValidation.coordinates(new double[]{90, -180}));
    }

    @Test public void altitudeMayBeZeroOrBelowSeaLevel() {
        assertTrue(GpsValueValidation.finite(0));
        assertTrue(GpsValueValidation.finite(-100));
        assertFalse(GpsValueValidation.finite(Double.NaN));
        assertFalse(GpsValueValidation.finite(Double.NEGATIVE_INFINITY));
    }
}
