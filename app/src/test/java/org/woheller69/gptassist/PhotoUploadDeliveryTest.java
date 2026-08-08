package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public class PhotoUploadDeliveryTest {
    @Test public void returnsOnlySelectedImagesInSelectionOrder() {
        PhotoUploadDelivery<String> result = new PhotoUploadDelivery<>(Arrays.asList("proxy-1", "proxy-2", "proxy-3"), "context");
        assertEquals(Arrays.asList("proxy-1", "proxy-2", "proxy-3"), result.attachments);
        assertEquals(3, result.attachments.size());
        assertFalse(result.attachments.contains("PHOTO_CONTEXT.txt"));
        assertEquals("context", result.context);
    }
}
