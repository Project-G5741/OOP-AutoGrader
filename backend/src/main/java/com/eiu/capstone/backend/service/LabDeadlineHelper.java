package com.eiu.capstone.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class LabDeadlineHelper {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Pattern LAB_NUMBER = Pattern.compile("(?i)lab\\s*(\\d+)");

    public enum UrgencyState {
        NONE,
        OK,
        WARNING,
        URGENT,
        EXPIRED
    }

    public Instant cutoffInstant(LocalDate deadlineDate) {
        if (deadlineDate == null) {
            return null;
        }
        ZonedDateTime endOfDay = deadlineDate.atTime(23, 59, 59).atZone(VIETNAM_ZONE);
        return endOfDay.toInstant();
    }

    public UrgencyState urgencyState(LocalDate deadlineDate, Instant now) {
        if (deadlineDate == null) {
            return UrgencyState.NONE;
        }
        Instant cutoff = cutoffInstant(deadlineDate);
        if (now.isAfter(cutoff)) {
            return UrgencyState.EXPIRED;
        }
        Duration remaining = Duration.between(now, cutoff);
        long hours = remaining.toHours();
        if (hours <= 24) {
            return UrgencyState.URGENT;
        }
        if (hours <= 72) {
            return UrgencyState.WARNING;
        }
        return UrgencyState.OK;
    }

    public boolean submissionCountsForLecturer(LocalDate deadlineDate, Instant submittedAt) {
        if (deadlineDate == null || submittedAt == null) {
            return submittedAt != null;
        }
        Instant cutoff = cutoffInstant(deadlineDate);
        return !submittedAt.isAfter(cutoff);
    }

    public Comparator<String> naturalLabNameComparator() {
        return Comparator.comparingInt(this::labSortKey).thenComparing(String::compareToIgnoreCase);
    }

    private int labSortKey(String name) {
        if (name == null) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = LAB_NUMBER.matcher(name.trim());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return Integer.MAX_VALUE - 1;
    }
}
