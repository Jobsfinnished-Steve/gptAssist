package org.woheller69.gptassist;

/** Retains post-selection work while a permission dialog is active. */
final class PendingPermissionRegistry<T> {
    static final class Pending<T> {
        final long generation;
        final T value;
        Pending(long generation, T value) { this.generation = generation; this.value = value; }
    }

    private Pending<T> pending;

    synchronized void retain(long generation, T value) {
        pending = new Pending<>(generation, value);
    }

    synchronized Pending<T> consume(long generation) {
        if (pending == null || pending.generation != generation) return null;
        Pending<T> result = pending;
        pending = null;
        return result;
    }

    synchronized void clear() { pending = null; }
}
