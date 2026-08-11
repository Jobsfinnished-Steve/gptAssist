package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaUriResolverTest {
    @Test public void parsesMediaDocumentsImageId() {
        assertEquals("12345", MediaUriResolver.mediaImageId("image:12345"));
    }

    @Test public void identifiesOnlyMediaDocumentsAuthority() {
        assertTrue(MediaUriResolver.isMediaDocumentsAuthority("com.android.providers.media.documents"));
        assertFalse(MediaUriResolver.isMediaDocumentsAuthority("com.example.documents"));
    }

    @Test public void distinguishesMediaStoreFromOtherProviders() {
        assertTrue(MediaUriResolver.supportsRequireOriginal("content", "media"));
        assertTrue(MediaUriResolver.supportsRequireOriginal("content", "com.android.providers.media.documents"));
        assertFalse(MediaUriResolver.supportsRequireOriginal("content", "com.example.documents"));
        assertFalse(MediaUriResolver.supportsRequireOriginal("file", "media"));
    }

    @Test public void rejectsNonImageAndMalformedDocumentIds() {
        assertNull(MediaUriResolver.mediaImageId("video:12345"));
        assertNull(MediaUriResolver.mediaImageId("image:"));
        assertNull(MediaUriResolver.mediaImageId(null));
    }
}
