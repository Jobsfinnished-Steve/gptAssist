package org.woheller69.gptassist;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class PhotoLocationPermissionPolicyTest {
    @Test public void preScopedStorageUsesOnlyDocumentGrant() {
        assertArrayEquals(new String[0], PhotoLocationPermissionPolicy.permissionsFor(21));
        assertArrayEquals(new String[0], PhotoLocationPermissionPolicy.permissionsFor(28));
    }

    @Test public void android10Through12RequestsStorageAndLocationTogether() {
        String[] expected = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.ACCESS_MEDIA_LOCATION"};
        assertArrayEquals(expected, PhotoLocationPermissionPolicy.permissionsFor(29));
        assertArrayEquals(expected, PhotoLocationPermissionPolicy.permissionsFor(32));
    }

    @Test public void android13RequestsImagesAndLocationTogether() {
        assertArrayEquals(new String[]{"android.permission.READ_MEDIA_IMAGES", "android.permission.ACCESS_MEDIA_LOCATION"},
                PhotoLocationPermissionPolicy.permissionsFor(33));
    }

    @Test public void android14AndLaterIncludeSelectedPhotosInSameRequest() {
        String[] expected = {"android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED", "android.permission.ACCESS_MEDIA_LOCATION"};
        assertArrayEquals(expected, PhotoLocationPermissionPolicy.permissionsFor(34));
        assertArrayEquals(expected, PhotoLocationPermissionPolicy.permissionsFor(35));
        assertArrayEquals(expected, PhotoLocationPermissionPolicy.permissionsFor(36));
    }
}
