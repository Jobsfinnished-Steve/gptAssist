package org.woheller69.gptassist;

/** Keeps an asynchronous result paired with the callback that originated it. */
public final class CallbackRequestRegistry<T> {
    public interface Recipient<T> { void accept(T value); }

    public static final class Request<T> {
        private final long generation;
        private final Recipient<T> recipient;
        private boolean completed;

        private Request(long generation, Recipient<T> recipient) {
            this.generation = generation;
            this.recipient = recipient;
        }

        public long getGeneration() { return generation; }
    }

    private long generation;
    private Request<T> active;

    public synchronized Request<T> begin(Recipient<T> recipient) {
        cancelActive();
        active = new Request<>(++generation, recipient);
        return active;
    }

    public synchronized boolean complete(Request<T> request, T value) {
        if (request == null || request != active || request.completed) return false;
        request.completed = true;
        active = null;
        request.recipient.accept(value);
        return true;
    }

    public synchronized void cancelActive() {
        Request<T> request = active;
        active = null;
        if (request != null && !request.completed) {
            request.completed = true;
            request.recipient.accept(null);
        }
    }
}
