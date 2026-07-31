package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class PhotoContextFormatterTest {
    private static PhotoContext full() {
        return new PhotoContext("2026-07-30T18:42:13.12", "+09:00",
                PhotoContext.TimestampSource.EXIF_DATETIME_ORIGINAL,
                12.3456789, -98.1, PhotoContext.MediaType.CAMERA_PHOTO);
    }

    @Test public void formatsSinglePhotoAndRequiredOrdering() {
        String text = PhotoContextFormatter.format(Collections.singletonList(full()));
        assertTrue(text.contains("[IMAGE_001]\ncaptured_at=2026-07-30T18:42:13.12\nutc_offset=+09:00\n"
                + "timestamp_source=EXIF_DATETIME_ORIGINAL\ngps=12.345679,-98.1\nmedia_type=CAMERA_PHOTO\n"));
        assertTrue(text.endsWith("\n"));
        assertFalse(text.contains("\r"));
    }

    @Test public void formatsMultipleInSelectionOrderWithPadding() {
        String text = PhotoContextFormatter.format(Arrays.asList(full(), PhotoContext.unknown(), full()));
        assertTrue(text.indexOf("IMAGE_001") < text.indexOf("IMAGE_002"));
        assertTrue(text.indexOf("IMAGE_002") < text.indexOf("IMAGE_003"));
        assertTrue(text.contains("gps=none\nmedia_type=UNKNOWN"));
        assertEquals(3, count(text, "[IMAGE_"));
    }

    @Test public void usesDotInNonUsLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertTrue(PhotoContextFormatter.format(Collections.singletonList(full())).contains("gps=12.345679,-98.1"));
        } finally { Locale.setDefault(old); }
    }

    @Test public void oneFailedPhotoDoesNotRemoveOthers() {
        String text = PhotoContextFormatter.format(Arrays.asList(full(), PhotoContext.unknown()));
        assertTrue(text.contains("timestamp_source=EXIF_DATETIME_ORIGINAL"));
        assertTrue(text.contains("timestamp_source=UNKNOWN"));
    }

    private static int count(String value, String needle) {
        int result = 0, offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) { result++; offset += needle.length(); }
        return result;
    }
}
