package org.woheller69.gptassist;

import static org.junit.Assert.*;

import org.junit.Test;

public class PhotoPermissionDecisionTest {
    @Test public void actualImageTriggersRequestRegardlessOfEmptyOrWildcardAcceptHint() {
        // acceptTypes is deliberately not an input: only the resolved selected URI type matters.
        assertTrue(PhotoPermissionDecision.shouldRequest(true, true, true, true, false));
    }

    @Test public void grantedPermissionDoesNotRequestAgain() {
        assertFalse(PhotoPermissionDecision.shouldRequest(true, true, true, true, true));
    }

    @Test public void disabledGpsOrContextAndPreQNeverRequest() {
        assertFalse(PhotoPermissionDecision.shouldRequest(false, true, true, true, false));
        assertFalse(PhotoPermissionDecision.shouldRequest(true, false, true, true, false));
        assertFalse(PhotoPermissionDecision.shouldRequest(true, true, true, false, false));
    }

    @Test public void nonImageAndMixedSelectionRemainOrdinaryUpload() {
        assertFalse(PhotoPermissionDecision.shouldRequest(true, true, false, true, false));
    }
}
