package org.woheller69.gptassist;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ExifDateParser {
    private ExifDateParser() {}

    public static String parse(String value, String subsecond) {
        if (value == null) return null;
        SimpleDateFormat input = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        input.setLenient(false);
        try {
            Date parsed = input.parse(value);
            String normalized = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(parsed);
            if (subsecond != null && subsecond.matches("\\d+")) normalized += "." + subsecond;
            return normalized;
        } catch (ParseException ignored) {
            return null;
        }
    }

    public static String normalizeOffset(String value) {
        return value != null && value.matches("[+-](0\\d|1\\d|2[0-3]):[0-5]\\d") ? value : "unknown";
    }
}
