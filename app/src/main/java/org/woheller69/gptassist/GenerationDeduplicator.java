package org.woheller69.gptassist;

final class GenerationDeduplicator {
    private long last = Long.MIN_VALUE;
    synchronized boolean markIfNew(long generation) {
        if (generation == last) return false;
        last = generation;
        return true;
    }
}
