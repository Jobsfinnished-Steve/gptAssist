package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaUriResolverTest {
    @Test public void parsesMediaDocumentsImageId() {
        assertEquals("12345", MediaUriResolver.mediaImageId("image:12345"));
    }

    @Test public void rejectsNonImageAndMalformedDocumentIds() {
        assertNull(MediaUriResolver.mediaImageId("video:12345"));
        assertNull(MediaUriResolver.mediaImageId("image:"));
        assertNull(MediaUriResolver.mediaImageId(null));
    }
}
