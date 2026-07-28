package com.eiu.capstone.backend.utility;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtil {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeUtil() {}

    public static OffsetDateTime nowInVietnam() {
        return OffsetDateTime.now(VIETNAM_ZONE);
    }
}