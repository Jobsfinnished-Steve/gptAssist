package org.woheller69.gptassist;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
public class UploadProxyStoreTest {
    @Test public void appendsTxtAndSanitizesTraversal() {
        assertEquals("photo.jpg.txt", UploadProxyStore.disguisedName("photo.jpg", 0, ".jpg"));
        String name = UploadProxyStore.disguisedName("../../evil.jpg", 0, ".jpg");
        assertFalse(name.contains("/"));
        assertTrue(name.endsWith(".jpg.txt"));
    }
    @Test public void deterministicFallbackHasImageIndex() {
        assertEquals("image_002.jpg.txt", UploadProxyStore.disguisedName(null, 1, ".jpg"));
    }
    @Test public void copiesBytesWithoutModification() throws Exception {
        byte[] source = new byte[]{(byte) 0xff, (byte) 0xd8, 0, 1, 2, (byte) 0xff, (byte) 0xd9};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        UploadProxyStore.copy(new ByteArrayInputStream(source), output, null);
        assertArrayEquals(source, output.toByteArray());
    }
    @Test public void cleanupKeepsCurrentOperation() throws Exception {
        File root = Files.createTempDirectory("proxy-test").toFile();
        File current = new File(root, "current"); assertTrue(current.mkdir());
        File old = new File(root, "old"); assertTrue(old.mkdir());
        assertTrue(old.setLastModified(1));
        UploadProxyStore.cleanup(root, current, 2L * 24L * 60L * 60L * 1000L);
        assertTrue(current.exists());
        assertFalse(old.exists());
    }
}
