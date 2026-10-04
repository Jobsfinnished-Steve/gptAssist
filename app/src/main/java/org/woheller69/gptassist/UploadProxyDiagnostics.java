package org.woheller69.gptassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class UploadProxyDiagnostics {
    final UploadProxyMimeMode mode;
    final int selectedCount;
    final int proxyCount;
    final String reportedMime;
    final boolean bytesIdentical;
    final boolean callbackCompleted;
    final List<Item> items;
    UploadProxyDiagnostics(UploadProxyMimeMode mode, int selectedCount, int proxyCount,
                           String reportedMime, boolean bytesIdentical, boolean callbackCompleted, List<Item> items) {
        this.mode = mode; this.selectedCount = selectedCount; this.proxyCount = proxyCount;
        this.reportedMime = reportedMime; this.bytesIdentical = bytesIdentical;
        this.callbackCompleted = callbackCompleted;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    boolean hasMissingGps() {
        for (Item item : items) if (!Boolean.TRUE.equals(item.coordinates)) return true;
        return false;
    }

    String itemSummary() {
        StringBuilder text = new StringBuilder();
        for (Item item : items) {
            text.append("\n\nImage ").append(item.index)
                    .append("\nOriginal access: ").append(item.status.name())
                    .append("\nProxy bytes verified: ").append(item.identical ? "YES" : "NO")
                    .append("\nProxy GPS coordinates readable: ").append(label(item.coordinates))
                    .append("\nProxy GPS altitude readable: ").append(label(item.altitude));
        }
        return text.toString();
    }

    private static String label(Boolean value) {
        return value == null ? "NOT_CHECKED" : (value ? "YES" : "NO");
    }

    static final class Item {
        final int index;
        final OriginalMediaResolver.Status status;
        final boolean identical;
        final Boolean coordinates;
        final Boolean altitude;
        Item(int index, OriginalMediaResolver.Status status, boolean identical,
             Boolean coordinates, Boolean altitude) {
            this.index = index; this.status = status; this.identical = identical;
            this.coordinates = coordinates; this.altitude = altitude;
        }
    }
}
