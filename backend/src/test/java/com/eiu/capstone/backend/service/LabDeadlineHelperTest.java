package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LabDeadlineHelperTest {

    private LabDeadlineHelper helper;

    @BeforeEach
    void setUp() {
        helper = new LabDeadlineHelper();
    }

    @Test
    void cutoffInstant_isEndOfDayVietnam() {
        Instant cutoff = helper.cutoffInstant(LocalDate.of(2026, 8, 15));
        Instant expected = LocalDate.of(2026, 8, 15)
                .atTime(23, 59, 59)
                .atZone(LabDeadlineHelper.VIETNAM_ZONE)
                .toInstant();
        assertEquals(expected, cutoff);
    }

    @Test
    void urgencyState_boundaries() {
        LocalDate deadline = LocalDate.of(2026, 8, 15);
        Instant cutoff = helper.cutoffInstant(deadline);

        assertEquals(LabDeadlineHelper.UrgencyState.EXPIRED,
                helper.urgencyState(deadline, cutoff.plusSeconds(1)));
        assertEquals(LabDeadlineHelper.UrgencyState.URGENT,
                helper.urgencyState(deadline, cutoff.minus(DurationHours(24))));
        assertEquals(LabDeadlineHelper.UrgencyState.WARNING,
                helper.urgencyState(deadline, cutoff.minus(DurationHours(72))));
        assertEquals(LabDeadlineHelper.UrgencyState.OK,
                helper.urgencyState(deadline, cutoff.minus(DurationHours(73))));
        assertEquals(LabDeadlineHelper.UrgencyState.NONE,
                helper.urgencyState(null, Instant.now()));
    }

    @Test
    void naturalLabNameComparator_ordersNumericPrefix() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>(
                java.util.List.of("Lab 10", "Lab 2", "Lab 1"));
        names.sort(helper.naturalLabNameComparator());
        assertEquals(java.util.List.of("Lab 1", "Lab 2", "Lab 10"), names);
    }

    @Test
    void submissionCountsForLecturer_respectsCutoff() {
        LocalDate deadline = LocalDate.of(2026, 8, 15);
        Instant before = helper.cutoffInstant(deadline).minusSeconds(60);
        Instant after = helper.cutoffInstant(deadline).plusSeconds(60);
        assertTrue(helper.submissionCountsForLecturer(deadline, before));
        assertTrue(!helper.submissionCountsForLecturer(deadline, after));
        assertTrue(helper.submissionCountsForLecturer(null, after));
    }

    private static java.time.Duration DurationHours(long hours) {
        return java.time.Duration.ofHours(hours);
    }
}
