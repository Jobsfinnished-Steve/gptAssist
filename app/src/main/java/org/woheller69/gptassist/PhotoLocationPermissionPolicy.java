package org.woheller69.gptassist;

import android.Manifest;

/** The photo permission group must be requested together with media location. */
final class PhotoLocationPermissionPolicy {
    private PhotoLocationPermissionPolicy() {}

    static String[] permissionsFor(int sdk) {
        if (sdk >= 34) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                    Manifest.permission.ACCESS_MEDIA_LOCATION};
        }
        if (sdk >= 33) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.ACCESS_MEDIA_LOCATION};
        }
        if (sdk >= 29) {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION};
        }
        // Older devices can read the selected document using its per-file grant.
        return new String[0];
    }
}
