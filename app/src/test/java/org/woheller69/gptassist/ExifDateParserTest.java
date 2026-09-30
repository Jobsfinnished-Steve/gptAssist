package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;

public class ExifDateParserTest {
    @Test public void parsesExifCalendarWithoutInventingOffset() {
        assertEquals("2026-07-30T18:42:13", ExifDateParser.parse("2026:07:30 18:42:13", null));
    }
    @Test public void preservesSubseconds() {
        assertEquals("2026-07-30T18:42:13.007", ExifDateParser.parse("2026:07:30 18:42:13", "007"));
    }
    @Test public void rejectsInvalidDate() {
        assertNull(ExifDateParser.parse("2026:99:30 18:42:13", null));
        assertNull(ExifDateParser.parse(null, null));
    }
    @Test public void validatesPresentAndAbsentUtcOffset() {
        assertEquals("+09:00", ExifDateParser.normalizeOffset("+09:00"));
        assertEquals("unknown", ExifDateParser.normalizeOffset(null));
        assertEquals("unknown", ExifDateParser.normalizeOffset("+99:00"));
    }
    @Test public void modelSupportsEveryFallbackSourceAndMediaType() {
        for (PhotoContext.TimestampSource source : PhotoContext.TimestampSource.values()) assertNotNull(source.name());
        assertArrayEquals(new String[]{"CAMERA_PHOTO", "SCREENSHOT", "DOWNLOADED", "EDITED", "UNKNOWN"},
                java.util.Arrays.stream(PhotoContext.MediaType.values()).map(Enum::name).toArray(String[]::new));
        assertEquals("unknown", PhotoContext.unknown().utcOffset);
    }
}
