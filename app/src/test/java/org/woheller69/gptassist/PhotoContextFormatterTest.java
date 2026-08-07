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
                12.3456789, -98.1, 42.34, PhotoContext.MediaType.CAMERA_PHOTO);
    }

    @Test public void formatsSinglePhotoAndRequiredOrdering() {
        String text = PhotoContextFormatter.format(Collections.singletonList(full()));
        assertTrue(text.contains("[IMAGE_001]\ncaptured_at=2026-07-30T18:42:13.12\nutc_offset=+09:00\n"
                + "timestamp_source=EXIF_DATETIME_ORIGINAL\ngps=12.345679,-98.1\ngps_altitude_m=42.3\ngps_status=AVAILABLE\nmedia_type=CAMERA_PHOTO\n"));
        assertTrue(text.endsWith("\n"));
        assertFalse(text.contains("\r"));
    }

    @Test public void formatsMultipleInSelectionOrderWithPadding() {
        String text = PhotoContextFormatter.format(Arrays.asList(full(), PhotoContext.unknown(), full()));
        assertTrue(text.indexOf("IMAGE_001") < text.indexOf("IMAGE_002"));
        assertTrue(text.indexOf("IMAGE_002") < text.indexOf("IMAGE_003"));
        assertTrue(text.contains("gps=none\ngps_altitude_m=none\ngps_status=READ_ERROR\nmedia_type=UNKNOWN"));
        assertEquals(3, count(text, "[IMAGE_"));
    }

    @Test public void usesDotInNonUsLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String text = PhotoContextFormatter.format(Collections.singletonList(full()));
            assertTrue(text.contains("gps=12.345679,-98.1"));
            assertTrue(text.contains("gps_altitude_m=42.3"));
        } finally { Locale.setDefault(old); }
    }

    @Test public void formatsAltitudeAboveBelowAndInteger() {
        PhotoContext below = new PhotoContext("unknown", "unknown", PhotoContext.TimestampSource.UNKNOWN,
                null, null, -12.5, PhotoContext.MediaType.UNKNOWN);
        PhotoContext integer = new PhotoContext("unknown", "unknown", PhotoContext.TimestampSource.UNKNOWN,
                null, null, 42.0, PhotoContext.MediaType.UNKNOWN);
        String text = PhotoContextFormatter.format(Arrays.asList(below, integer));
        assertTrue(text.contains("gps_altitude_m=-12.5"));
        assertTrue(text.contains("gps_altitude_m=42"));
    }

    @Test public void missingOrNonFiniteAltitudeIsNone() {
        PhotoContext missing = new PhotoContext("unknown", "unknown", PhotoContext.TimestampSource.UNKNOWN,
                1.0, 2.0, Double.NaN, PhotoContext.MediaType.UNKNOWN);
        assertTrue(PhotoContextFormatter.format(Collections.singletonList(missing))
                .contains("gps=1,2\ngps_altitude_m=none\ngps_status=AVAILABLE\nmedia_type=UNKNOWN"));
    }

    @Test public void versionThreeAndExactlyOneFinalNewline() {
        String text = PhotoContextFormatter.format(Collections.singletonList(full()));
        assertTrue(text.startsWith("PHOTO_CONTEXT v3\n"));
        assertTrue(text.endsWith("\n"));
        assertFalse(text.endsWith("\n\n"));
    }


    @Test public void formatsGpsStatusesImmediatelyAfterAltitude() {
        for (PhotoContext.GpsReadStatus status : new PhotoContext.GpsReadStatus[]{
                PhotoContext.GpsReadStatus.AVAILABLE,
                PhotoContext.GpsReadStatus.PERMISSION_DENIED,
                PhotoContext.GpsReadStatus.ORIGINAL_ACCESS_FAILED,
                PhotoContext.GpsReadStatus.NO_GPS_TAG,
                PhotoContext.GpsReadStatus.GPS_DISABLED}) {
            PhotoContext photo = new PhotoContext("unknown", "unknown", PhotoContext.TimestampSource.UNKNOWN,
                    null, null, null, PhotoContext.MediaType.UNKNOWN, status);
            String text = PhotoContextFormatter.format(Collections.singletonList(photo));
            assertTrue(text.contains("gps_altitude_m=none\ngps_status=" + status.name() + "\nmedia_type="));
        }
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
