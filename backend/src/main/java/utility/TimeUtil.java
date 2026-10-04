package com.eiu.capstone.backend.utility;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final DateTimeFormatter LATEST_SUBMISSION_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private TimeUtil() {}

    public static OffsetDateTime nowInVietnam() {
        return OffsetDateTime.now(VIETNAM_ZONE);
    }

    public static String formatLatestSubmission(OffsetDateTime value) {
        return value == null ? null : LATEST_SUBMISSION_FORMAT.format(value);
    }
}