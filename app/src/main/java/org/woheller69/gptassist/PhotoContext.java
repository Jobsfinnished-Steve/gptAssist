package org.woheller69.gptassist;

public final class PhotoContext {
    public enum TimestampSource { EXIF_DATETIME_ORIGINAL, MEDIASTORE_DATE_TAKEN, FILE_LAST_MODIFIED, UNKNOWN }
    public enum MediaType { CAMERA_PHOTO, SCREENSHOT, DOWNLOADED, EDITED, UNKNOWN }
    public enum GpsReadStatus { AVAILABLE, GPS_DISABLED, PERMISSION_DENIED, NO_GPS_TAG, ORIGINAL_ACCESS_FAILED, UNSUPPORTED_PROVIDER, READ_ERROR }

    public final String capturedAt;
    public final String utcOffset;
    public final TimestampSource timestampSource;
    public final Double latitude;
    public final Double longitude;
    public final Double gpsAltitudeMeters;
    public final MediaType mediaType;
    public final GpsReadStatus gpsReadStatus;

    public PhotoContext(String capturedAt, String utcOffset, TimestampSource timestampSource,
                        Double latitude, Double longitude, Double gpsAltitudeMeters, MediaType mediaType) {
        this(capturedAt, utcOffset, timestampSource, latitude, longitude, gpsAltitudeMeters, mediaType,
                latitude != null && longitude != null ? GpsReadStatus.AVAILABLE : GpsReadStatus.NO_GPS_TAG);
    }

    public PhotoContext(String capturedAt, String utcOffset, TimestampSource timestampSource,
                        Double latitude, Double longitude, Double gpsAltitudeMeters, MediaType mediaType,
                        GpsReadStatus gpsReadStatus) {
        this.capturedAt = capturedAt == null ? "unknown" : capturedAt;
        this.utcOffset = utcOffset == null ? "unknown" : utcOffset;
        this.timestampSource = timestampSource == null ? TimestampSource.UNKNOWN : timestampSource;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gpsAltitudeMeters = gpsAltitudeMeters != null && !Double.isNaN(gpsAltitudeMeters) && !Double.isInfinite(gpsAltitudeMeters) ? gpsAltitudeMeters : null;
        this.mediaType = mediaType == null ? MediaType.UNKNOWN : mediaType;
        this.gpsReadStatus = gpsReadStatus == null ? GpsReadStatus.READ_ERROR : gpsReadStatus;
    }

    public static PhotoContext unknown() {
        return new PhotoContext("unknown", "unknown", TimestampSource.UNKNOWN, null, null, null,
                MediaType.UNKNOWN, GpsReadStatus.READ_ERROR);
    }
}
