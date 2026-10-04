package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public class OriginalMediaResolverTest {
    @Test public void parsesOnlyNumericImageDocumentIds() {
        assertEquals(Long.valueOf(12345), OriginalMediaResolver.numericImageId("image:12345"));
        assertNull(OriginalMediaResolver.numericImageId("video:12345"));
        assertNull(OriginalMediaResolver.numericImageId("image:12/../34"));
        assertNull(OriginalMediaResolver.numericImageId("image:not-a-number"));
        assertNull(OriginalMediaResolver.numericImageId(null));
    }

    @Test public void recognizesCanonicalMediaStoreImagePathOnly() {
        assertTrue(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("external", "images", "media", "42")));
        assertFalse(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("picker", "images", "media", "42")));
        assertFalse(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("external", "images", "media", "not-numeric")));
        assertTrue(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("1234-ABCD", "images", "media", "42")));
        assertTrue(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("external_primary", "file", "42")));
        assertFalse(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("picker", "file", "42")));
        assertFalse(OriginalMediaResolver.isDirectMediaStoreImagePath(
                Arrays.asList("external", "file", "-1")));
    }

    @Test public void distinguishesFullLimitedAndDeniedAccess() {
        assertEquals(OriginalMediaResolver.MediaAccess.FULL,
                OriginalMediaResolver.classifyMediaAccess(true, true));
        assertEquals(OriginalMediaResolver.MediaAccess.LIMITED,
                OriginalMediaResolver.classifyMediaAccess(false, true));
        assertEquals(OriginalMediaResolver.MediaAccess.DENIED,
                OriginalMediaResolver.classifyMediaAccess(false, false));
    }
}
