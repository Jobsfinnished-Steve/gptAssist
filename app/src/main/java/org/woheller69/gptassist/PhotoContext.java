package org.woheller69.gptassist;

public final class PhotoContext {
    public enum TimestampSource { EXIF_DATETIME_ORIGINAL, MEDIASTORE_DATE_TAKEN, FILE_LAST_MODIFIED, UNKNOWN }
    public enum MediaType { CAMERA_PHOTO, SCREENSHOT, DOWNLOADED, EDITED, UNKNOWN }

    public final String capturedAt;
    public final String utcOffset;
    public final TimestampSource timestampSource;
    public final Double latitude;
    public final Double longitude;
    public final MediaType mediaType;

    public PhotoContext(String capturedAt, String utcOffset, TimestampSource timestampSource,
                        Double latitude, Double longitude, MediaType mediaType) {
        this.capturedAt = capturedAt == null ? "unknown" : capturedAt;
        this.utcOffset = utcOffset == null ? "unknown" : utcOffset;
        this.timestampSource = timestampSource == null ? TimestampSource.UNKNOWN : timestampSource;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mediaType = mediaType == null ? MediaType.UNKNOWN : mediaType;
    }

    public static PhotoContext unknown() {
        return new PhotoContext("unknown", "unknown", TimestampSource.UNKNOWN, null, null, MediaType.UNKNOWN);
    }
}
