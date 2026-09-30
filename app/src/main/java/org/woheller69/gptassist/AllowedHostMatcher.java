package org.woheller69.gptassist;

import java.util.List;
import java.util.Locale;

/** Exact-host or dot-delimited subdomain matching for the WebView allowlist. */
final class AllowedHostMatcher {
    private AllowedHostMatcher() {}

    static boolean isAllowed(String host, List<String> allowedDomains) {
        if (host == null) return false;
        String normalizedHost = host.toLowerCase(Locale.US);
        for (String domain : allowedDomains) {
            if (domain == null || domain.isEmpty()) continue;
            String normalizedDomain = domain.toLowerCase(Locale.US);
            if (normalizedHost.equals(normalizedDomain)
                    || normalizedHost.endsWith("." + normalizedDomain)) return true;
        }
        return false;
    }
}
