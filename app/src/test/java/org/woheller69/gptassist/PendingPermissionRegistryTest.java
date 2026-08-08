package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class PendingPermissionRegistryTest {
    @Test public void grantedOrDeniedSelectionResumesOnlyOnce() {
        PendingPermissionRegistry<String> registry = new PendingPermissionRegistry<>();
        registry.retain(1, "selected-image");
        assertEquals("selected-image", registry.consume(1).value);
        assertNull(registry.consume(1));
    }

    @Test public void stalePermissionResultCannotConsumeNewerUpload() {
        PendingPermissionRegistry<String> registry = new PendingPermissionRegistry<>();
        registry.retain(1, "first");
        registry.retain(2, "second");
        assertNull(registry.consume(1));
        assertEquals("second", registry.consume(2).value);
    }
}
