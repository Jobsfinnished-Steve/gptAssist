package org.woheller69.gptassist;

import static org.junit.Assert.*;
import org.junit.Test;

public class ComposerAppendPolicyTest {
    @Test public void preservesDraftAndAddsTwoNewlines() {
        assertEquals("my draft\n\nPHOTO_CONTEXT v3\nblock\n",
                ComposerAppendPolicy.append("my draft", "PHOTO_CONTEXT v3\nblock\n"));
    }
    @Test public void doesNotInsertSameBlockTwice() {
        String once = ComposerAppendPolicy.append("draft", "context");
        assertEquals(once, ComposerAppendPolicy.append(once, "context"));
    }
}
