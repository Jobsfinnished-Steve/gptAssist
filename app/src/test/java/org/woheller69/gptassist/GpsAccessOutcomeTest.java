package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;

public class GpsAccessOutcomeTest {
    @Test public void successfulOriginalStreamCanReportNoGpsTag() {
        assertEquals(PhotoContext.GpsReadStatus.NO_GPS_TAG,
                GpsAccessOutcome.status(true, false, true, true));
    }
    @Test public void ordinaryStreamAfterOriginalFailureIsDistinguished() {
        assertEquals(PhotoContext.GpsReadStatus.ORIGINAL_ACCESS_FAILED,
                GpsAccessOutcome.status(true, false, false, true));
    }
    @Test public void disabledAndDeniedAreExplicit() {
        assertEquals(PhotoContext.GpsReadStatus.GPS_DISABLED,
                GpsAccessOutcome.status(false, false, false, true));
        assertEquals(PhotoContext.GpsReadStatus.PERMISSION_DENIED,
                GpsAccessOutcome.status(false, true, false, true));
    }
}
