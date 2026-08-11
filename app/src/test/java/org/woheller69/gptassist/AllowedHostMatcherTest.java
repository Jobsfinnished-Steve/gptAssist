package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;

public class AllowedHostMatcherTest {
    private static final java.util.List<String> ALLOWED = Arrays.asList(
            "chatgpt.com", "accounts.google.com", "gstatic.com");

    @Test public void permitsExactGoogleLoginHostAndRealSubdomains() {
        assertTrue(AllowedHostMatcher.isAllowed("accounts.google.com", ALLOWED));
        assertTrue(AllowedHostMatcher.isAllowed("sub.accounts.google.com", ALLOWED));
        assertTrue(AllowedHostMatcher.isAllowed("ssl.gstatic.com", ALLOWED));
    }

    @Test public void rejectsSuffixConfusionAndUnrelatedGoogleHosts() {
        assertFalse(AllowedHostMatcher.isAllowed("evilaccounts.google.com.example", ALLOWED));
        assertFalse(AllowedHostMatcher.isAllowed("notchatgpt.com", ALLOWED));
        assertFalse(AllowedHostMatcher.isAllowed("mail.google.com", ALLOWED));
        assertFalse(AllowedHostMatcher.isAllowed(null, ALLOWED));
    }
}
