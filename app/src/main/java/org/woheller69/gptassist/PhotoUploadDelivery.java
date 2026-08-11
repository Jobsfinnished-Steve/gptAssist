package org.woheller69.gptassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PhotoUploadDelivery<T> {
    final List<T> attachments;
    final String context;
    PhotoUploadDelivery(List<T> selectedImages, String context) {
        this.attachments = Collections.unmodifiableList(new ArrayList<>(selectedImages));
        this.context = context;
    }
}
