package org.woheller69.gptassist;
import static org.junit.Assert.*;
import org.junit.Test;
public class UploadProxyMimeModeTest {
    @Test public void reportsAllThreeMimeModes() {
        assertEquals("text/plain", UploadProxyMimeMode.TEXT.reportedMime("image/heic"));
        assertEquals("application/octet-stream", UploadProxyMimeMode.OCTET_STREAM.reportedMime("image/jpeg"));
        assertEquals("image/jpeg", UploadProxyMimeMode.IMAGE_MIME.reportedMime("image/jpeg"));
        assertEquals("image/heic", UploadProxyMimeMode.IMAGE_MIME.reportedMime("image/heic"));
    }
    @Test public void textIsDefaultForUnknownPreference() {
        assertEquals(UploadProxyMimeMode.TEXT, UploadProxyMimeMode.fromPreference("bad"));
    }
}
