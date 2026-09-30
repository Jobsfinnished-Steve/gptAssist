package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;

public class PhotoChooserDecisionTest {
    @Test public void api36GpsImageLibraryUsesOpenDocument() {
        assertTrue(PhotoChooserDecision.useOpenDocument(true, true, true, false, 36));
    }
    @Test public void genericAndCaptureRequestsKeepExistingChooser() {
        assertFalse(PhotoChooserDecision.useOpenDocument(true, true, false, false, 36));
        assertFalse(PhotoChooserDecision.useOpenDocument(true, true, true, true, 36));
        assertFalse(PhotoChooserDecision.useOpenDocument(false, true, true, false, 36));
    }
}
