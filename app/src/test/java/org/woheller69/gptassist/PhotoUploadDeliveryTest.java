package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.Arrays;

public class PhotoUploadDeliveryTest {
    @Test public void returnsOnlySelectedImagesInSelectionOrder() {
        PhotoUploadDelivery<String> result = new PhotoUploadDelivery<>(Arrays.asList("image-1", "image-2"), "context");
        assertEquals(Arrays.asList("image-1", "image-2"), result.attachments);
        assertEquals(2, result.attachments.size());
        assertFalse(result.attachments.contains("PHOTO_CONTEXT.txt"));
        assertEquals("context", result.context);
    }
}
