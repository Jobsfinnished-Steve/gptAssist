package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CallbackRequestRegistryTest {
    @Test public void staleBackgroundResultCannotCompleteNewCallback() {
        CallbackRequestRegistry<String> registry = new CallbackRequestRegistry<>();
        List<String> firstValues = new ArrayList<>();
        List<String> secondValues = new ArrayList<>();

        CallbackRequestRegistry.Request<String> first = registry.begin(firstValues::add);
        CallbackRequestRegistry.Request<String> second = registry.begin(secondValues::add);

        assertEquals(1, firstValues.size());
        assertNull(firstValues.get(0));
        assertFalse(registry.complete(first, "stale result"));
        assertTrue(registry.complete(second, "current result"));
        assertEquals(java.util.Collections.singletonList("current result"), secondValues);
    }

    @Test public void callbackIsCompletedAtMostOnce() {
        CallbackRequestRegistry<String> registry = new CallbackRequestRegistry<>();
        List<String> values = new ArrayList<>();
        CallbackRequestRegistry.Request<String> request = registry.begin(values::add);

        assertTrue(registry.complete(request, "result"));
        assertFalse(registry.complete(request, "duplicate"));
        registry.cancelActive();
        assertEquals(java.util.Collections.singletonList("result"), values);
    }

    @Test public void cancelledRecoveryCannotStartOrCancelAnotherUpload() {
        CallbackRequestRegistry<String> registry = new CallbackRequestRegistry<>();
        List<String> firstValues = new ArrayList<>();
        List<String> secondValues = new ArrayList<>();
        CallbackRequestRegistry.Request<String> first = registry.begin(firstValues::add);
        assertTrue(registry.isActive(first));
        CallbackRequestRegistry.Request<String> second = registry.begin(secondValues::add);
        assertFalse(registry.isActive(first));
        assertTrue(registry.isActive(second));
        assertFalse(registry.complete(first, null));
        assertEquals(java.util.Collections.singletonList(null), firstValues);
        assertTrue(secondValues.isEmpty());
        registry.cancelActive();
        assertFalse(registry.isActive(second));
        assertFalse(registry.complete(second, "late permission result"));
        assertEquals(java.util.Collections.singletonList(null), secondValues);
        assertFalse(registry.isActive(null));
    }
}
