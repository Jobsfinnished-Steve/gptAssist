package org.woheller69.gptassist;

final class ComposerAppendPolicy {
    private ComposerAppendPolicy() {}
    static String append(String existing, String block) {
        String current = existing == null ? "" : existing;
        if (block == null || block.isEmpty() || current.contains(block)) return current;
        return current + (current.isEmpty() ? "" : "\n\n") + block;
    }
}
