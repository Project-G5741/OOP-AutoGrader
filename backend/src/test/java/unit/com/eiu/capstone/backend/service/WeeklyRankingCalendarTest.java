package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.service.WeeklyRankingCalendar;
import com.eiu.capstone.backend.utility.TimeUtil;

class WeeklyRankingCalendarTest {

    private static Instant vn(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, TimeUtil.VIETNAM_ZONE).toInstant();
    }

    @Test
    void weekRunsMondayStartThroughSundayEnd() {
        Instant wednesday = vn(2026, 10, 21, 10, 0, 0);
        Instant start = WeeklyRankingCalendar.weekStartOf(wednesday);
        Instant end = WeeklyRankingCalendar.weekEndOf(wednesday);
        assertEquals(vn(2026, 10, 19, 0, 0, 0), start);
        assertEquals(
                ZonedDateTime.of(LocalDate.of(2026, 10, 25), LocalTime.of(23, 59, 59), TimeUtil.VIETNAM_ZONE)
                        .toInstant(),
                end);
    }

    @Test
    void lastCompletedWeekIsPreviousSundayUntilThisSundayFreeze() {
        Instant wednesday = vn(2026, 10, 21, 10, 0, 0);
        Instant last = WeeklyRankingCalendar.lastCompletedWeekEnd(wednesday);
        assertEquals(
                ZonedDateTime.of(LocalDate.of(2026, 10, 18), LocalTime.of(23, 59, 59), TimeUtil.VIETNAM_ZONE)
                        .toInstant(),
                last);
        Instant sundayFreeze = WeeklyRankingCalendar.weekEndOf(wednesday);
        assertEquals(sundayFreeze, WeeklyRankingCalendar.lastCompletedWeekEnd(sundayFreeze));
    }

    @Test
    void completedWeekEndsDoNotIncludeTheLiveWeek() {
        Instant wednesday = vn(2026, 10, 21, 10, 0, 0);
        Instant from = WeeklyRankingCalendar.weekStartOf(wednesday);
        List<Instant> ends = WeeklyRankingCalendar.completedWeekEndsThrough(from, wednesday);
        assertTrue(ends.isEmpty());
    }
}
