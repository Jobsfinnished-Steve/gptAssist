package org.woheller69.gptassist;

import org.junit.Test;
import static org.junit.Assert.*;

public class PendingUploadPermissionTest {
    @Test public void cancelledUploadDoesNotReturnAfterPermissionResult() {
        PendingUploadPermission<String> pending = new PendingUploadPermission<>();
        assertTrue(pending.await("cancelled"));
        pending.clearSelection();
        assertNull(pending.finish());
    }

    @Test public void replacementUploadUsesOneDialogAndOnlyLatestSelection() {
        PendingUploadPermission<String> pending = new PendingUploadPermission<>();
        assertTrue(pending.await("old"));
        pending.clearSelection();
        assertFalse(pending.await("new"));
        assertEquals("new", pending.finish());
        assertNull(pending.finish());
    }

    @Test public void denialStillResumesOnceAndNextUploadCanRequestAgain() {
        PendingUploadPermission<String> pending = new PendingUploadPermission<>();
        assertTrue(pending.await("first"));
        assertEquals("first", pending.finish());
        assertNull(pending.finish());
        assertTrue(pending.await("second"));
        assertEquals("second", pending.finish());
    }
}
