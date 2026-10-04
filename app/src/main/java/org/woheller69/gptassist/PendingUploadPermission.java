package org.woheller69.gptassist;

/** Main-thread state: one system permission dialog, at most one current upload. */
final class PendingUploadPermission<T> {
    private boolean inFlight;
    private T pending;

    boolean await(T selection) {
        pending = selection;
        if (inFlight) return false;
        inFlight = true;
        return true;
    }

    T finish() {
        inFlight = false;
        T selection = pending;
        pending = null;
        return selection;
    }

    // The OS dialog may still return, but must never revive a cancelled upload.
    void clearSelection() { pending = null; }
}
