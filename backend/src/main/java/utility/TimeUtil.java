package com.eiu.capstone.backend.utility;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final DateTimeFormatter LATEST_SUBMISSION_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    public static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TimeUtil() {}

    public static OffsetDateTime nowInVietnam() {
        return OffsetDateTime.now(VIETNAM_ZONE);
    }

    /**
     * Formats an absolute instant for UI display in Vietnam time.
     * JDBC/Railway often surfaces timestamptz as UTC ({@code Z}); never format the
     * raw offset wall-clock or {@link ZoneId#systemDefault()}.
     */
    public static String formatDisplayTimestamp(OffsetDateTime value) {
        return value == null
                ? null
                : DISPLAY_TIMESTAMP_FORMAT.format(value.atZoneSameInstant(VIETNAM_ZONE));
    }

    /** Same contract as {@link #formatDisplayTimestamp(OffsetDateTime)} for JDBC row values. */
    public static String formatDisplayTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return formatDisplayTimestamp(offsetDateTime);
        }
        if (value instanceof Instant instant) {
            return DISPLAY_TIMESTAMP_FORMAT.format(instant.atZone(VIETNAM_ZONE));
        }
        if (value instanceof LocalDateTime localDateTime) {
            // Naive local values are treated as Vietnam wall clock (same as StatsRepository).
            return DISPLAY_TIMESTAMP_FORMAT.format(localDateTime);
        }
        return value.toString();
    }

    public static String formatLatestSubmission(OffsetDateTime value) {
        return value == null
                ? null
                : LATEST_SUBMISSION_FORMAT.format(value.atZoneSameInstant(VIETNAM_ZONE));
    }
}
