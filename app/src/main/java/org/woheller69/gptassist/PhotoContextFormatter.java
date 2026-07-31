package org.woheller69.gptassist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

public final class PhotoContextFormatter {
    private PhotoContextFormatter() {}

    public static String format(List<PhotoContext> photos) {
        StringBuilder out = new StringBuilder();
        out.append("PHOTO_CONTEXT v1\n")
                .append("This metadata describes the selected image attachments only.\n")
                .append("Images correspond to IMAGE_001, IMAGE_002, ... in the same order in which they were selected.\n")
                .append("PHOTO_CONTEXT.txt itself is not included in the image numbering.\n\n");
        for (int i = 0; i < photos.size(); i++) {
            PhotoContext photo = photos.get(i);
            out.append(String.format(Locale.US, "[IMAGE_%03d]\n", i + 1));
            out.append("captured_at=").append(photo.capturedAt).append('\n');
            out.append("utc_offset=").append(photo.utcOffset).append('\n');
            out.append("timestamp_source=").append(photo.timestampSource.name()).append('\n');
            out.append("gps=");
            if (photo.latitude == null || photo.longitude == null) out.append("none");
            else out.append(decimal(photo.latitude)).append(',').append(decimal(photo.longitude));
            out.append('\n').append("media_type=").append(photo.mediaType.name()).append('\n');
            if (i + 1 < photos.size()) out.append('\n');
        }
        return out.toString();
    }

    private static String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
