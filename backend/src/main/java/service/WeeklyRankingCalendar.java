package com.eiu.capstone.backend.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.eiu.capstone.backend.utility.TimeUtil;

/**
 * Vietnam-time contest week: Monday 00:00:00 through Sunday 23:59:59.
 */
public final class WeeklyRankingCalendar {

    private WeeklyRankingCalendar() {
    }

    public static Instant weekEndOf(Instant instant) {
        LocalDate monday = mondayOf(instant);
        return sundayEnd(monday);
    }

    public static Instant weekStartOf(Instant instant) {
        LocalDate monday = mondayOf(instant);
        return monday.atStartOfDay(TimeUtil.VIETNAM_ZONE).toInstant();
    }

    /**
     * Sundays whose 23:59:59 instant is strictly before {@code now}, oldest first.
     * Used for catch-up of unpaid weeks.
     */
    public static List<Instant> completedWeekEndsThrough(Instant fromInclusive, Instant now) {
        List<Instant> ends = new ArrayList<>();
        if (fromInclusive == null || now == null) {
            return ends;
        }
        Instant lastCompleted = lastCompletedWeekEnd(now);
        LocalDate monday = mondayOf(fromInclusive);
        Instant end = sundayEnd(monday);
        if (end.isBefore(fromInclusive)) {
            monday = monday.plusWeeks(1);
            end = sundayEnd(monday);
        }
        while (!end.isAfter(lastCompleted)) {
            ends.add(end);
            monday = monday.plusWeeks(1);
            end = sundayEnd(monday);
            if (ends.size() > 60) {
                break;
            }
        }
        return ends;
    }

    public static Instant lastCompletedWeekEnd(Instant now) {
        Instant thisEnd = weekEndOf(now);
        if (!now.isBefore(thisEnd)) {
            return thisEnd;
        }
        LocalDate monday = mondayOf(now).minusWeeks(1);
        return sundayEnd(monday);
    }

    private static LocalDate mondayOf(Instant instant) {
        ZonedDateTime z = instant.atZone(TimeUtil.VIETNAM_ZONE);
        return z.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static Instant sundayEnd(LocalDate monday) {
        LocalDate sunday = monday.plusDays(6);
        return ZonedDateTime.of(sunday, LocalTime.of(23, 59, 59), TimeUtil.VIETNAM_ZONE).toInstant();
    }
}
