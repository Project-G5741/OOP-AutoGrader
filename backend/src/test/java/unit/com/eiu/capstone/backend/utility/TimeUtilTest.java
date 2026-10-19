package unit.com.eiu.capstone.backend.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.utility.TimeUtil;

class TimeUtilTest {

    /** Same instant as the Railway bug: 21:31 Vietnam stored/read as 14:31Z. */
    private static final OffsetDateTime UTC_INSTANT =
            OffsetDateTime.parse("2026-10-19T14:31:00Z");

    @Test
    void formatDisplayTimestamp_convertsUtcOffsetDateTimeToVietnamWallClock() {
        assertEquals("19/10/2026 21:31", TimeUtil.formatDisplayTimestamp(UTC_INSTANT));
    }

    @Test
    void formatDisplayTimestamp_convertsInstantUsingVietnamZoneNotJvmDefault() {
        Instant instant = Instant.parse("2026-10-19T14:31:00Z");
        assertEquals("19/10/2026 21:31", TimeUtil.formatDisplayTimestamp(instant));
    }

    @Test
    void formatDisplayTimestamp_keepsVietnamOffsetWallClock() {
        OffsetDateTime vietnam = OffsetDateTime.parse("2026-10-19T21:31:00+07:00");
        assertEquals("19/10/2026 21:31", TimeUtil.formatDisplayTimestamp(vietnam));
    }

    @Test
    void formatDisplayTimestamp_treatsLocalDateTimeAsVietnamWallClock() {
        LocalDateTime local = LocalDateTime.of(2026, 10, 19, 21, 31);
        assertEquals("19/10/2026 21:31", TimeUtil.formatDisplayTimestamp(local));
    }

    @Test
    void formatLatestSubmission_convertsUtcOffsetToVietnamWallClock() {
        assertEquals("Oct 19, 2026 21:31", TimeUtil.formatLatestSubmission(UTC_INSTANT));
    }

    @Test
    void formatDisplayTimestamp_nullReturnsNull() {
        assertNull(TimeUtil.formatDisplayTimestamp((OffsetDateTime) null));
        assertNull(TimeUtil.formatDisplayTimestamp((Object) null));
        assertNull(TimeUtil.formatLatestSubmission(null));
    }
}
