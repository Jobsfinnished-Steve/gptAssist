package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class SelectedFileTypeTest {
    @Test public void resolverMimeTypeTakesPriority() {
        assertTrue(SelectedFileType.isImage("image/jpeg", "photo.bin"));
        assertFalse(SelectedFileType.isImage("application/pdf", "misleading.jpg"));
    }

    @Test public void safeImageExtensionsAreFallbackWhenMimeMissing() {
        assertTrue(SelectedFileType.isImage(null, "PHOTO.HEIC"));
        assertTrue(SelectedFileType.isImage("", "photo.png"));
        assertFalse(SelectedFileType.isImage(null, "document.pdf"));
        assertFalse(SelectedFileType.isImage(null, "archive.zip"));
        assertFalse(SelectedFileType.isImage(null, "notes.txt"));
        assertFalse(SelectedFileType.isImage(null, "unknown"));
    }

    @Test public void mixedSelectionDoesNotQualifyAsAllImages() {
        boolean allImages = SelectedFileType.isImage("image/jpeg", "one.jpg")
                && SelectedFileType.isImage("application/pdf", "two.pdf");
        assertFalse(allImages);
    }
}
