package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;

public class GenerationDeduplicatorTest {
    @Test public void composerInsertionOccursOncePerUploadGeneration() {
        GenerationDeduplicator generations = new GenerationDeduplicator();
        assertTrue(generations.markIfNew(10));
        assertFalse(generations.markIfNew(10));
        assertTrue(generations.markIfNew(11));
    }
}
