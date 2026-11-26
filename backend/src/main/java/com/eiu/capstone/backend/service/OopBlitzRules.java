package com.eiu.capstone.backend.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Pure scoring, pacing, and Learn-to-Unlock rules for OOP Blitz.
 */
public final class OopBlitzRules {

    public static final String MODE_REWARDED = "REWARDED";
    public static final String MODE_PRACTICE = "PRACTICE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public static final int QUESTIONS_PER_MATCH = 8;
    public static final int FREE_DAILY_RUNS = 1;
    public static final int MAX_SUBMIT_UNLOCK_RUNS = 2;
    public static final int MAX_DAILY_REWARDED_RUNS = FREE_DAILY_RUNS + MAX_SUBMIT_UNLOCK_RUNS;
    public static final int DAILY_POINTS_CAP = 250;
    public static final int MAX_POINTS_PER_MATCH = 100;

    public static final long NETWORK_GRACE_MS = 2_000L;
    public static final long FEEDBACK_HOLD_MS = 1_800L;
    public static final long SPEED_MIN_ELAPSED_MS = 700L;
    public static final double SPEED_MAX_RATIO = 0.40;
    public static final long SESSION_STALE_MINUTES = 15L;

    private OopBlitzRules() {
    }

    public static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_REWARDED;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (MODE_REWARDED.equals(normalized) || MODE_PRACTICE.equals(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OOP Blitz mode");
    }

    public static int allowedRewardedRuns(int submitCountToday) {
        return FREE_DAILY_RUNS + Math.min(MAX_SUBMIT_UNLOCK_RUNS, Math.max(0, submitCountToday));
    }

    public static int basePoints(String difficulty) {
        if ("HARD".equals(difficulty)) {
            return 10;
        }
        if ("MEDIUM".equals(difficulty)) {
            return 8;
        }
        return 6;
    }

    public static int comboBonus(int combo) {
        if (combo <= 1) {
            return 0;
        }
        return Math.min(4, combo - 1);
    }

    public static long holdOffsetMs(int questionIndex) {
        return questionIndex > 0 ? FEEDBACK_HOLD_MS : 0L;
    }

    public static boolean isTimedOut(long rawElapsedMs, int timeLimitSec, int questionIndex) {
        long limitMs = timeLimitSec * 1000L;
        return rawElapsedMs > limitMs + holdOffsetMs(questionIndex) + NETWORK_GRACE_MS;
    }

    public static int speedBonus(long rawElapsedMs, int timeLimitSec, int questionIndex) {
        if (rawElapsedMs < SPEED_MIN_ELAPSED_MS) {
            return 0;
        }
        long effectiveMs = Math.max(0L, rawElapsedMs - holdOffsetMs(questionIndex));
        long thresholdMs = (long) (timeLimitSec * 1000L * SPEED_MAX_RATIO);
        return effectiveMs <= thresholdMs ? 2 : 0;
    }
}
