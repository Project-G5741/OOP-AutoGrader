package com.eiu.capstone.backend.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AtRiskScoreTest {

    @Test
    void flagsOnlyScoreTotalsBelowFifty() {
        assertTrue(LecturerAnalyticsService.isAtRiskScore(new BigDecimal("0.00")));
        assertTrue(LecturerAnalyticsService.isAtRiskScore(new BigDecimal("49.99")));
        assertFalse(LecturerAnalyticsService.isAtRiskScore(new BigDecimal("50.00")));
        assertFalse(LecturerAnalyticsService.isAtRiskScore(new BigDecimal("50.01")));
        assertFalse(LecturerAnalyticsService.isAtRiskScore(new BigDecimal("100.00")));
        assertFalse(LecturerAnalyticsService.isAtRiskScore(null));
    }

    @Test
    void roundsTheScoreTotalDownBeforeTheCutoff() {
        assertEquals(new BigDecimal("49.99"), LecturerAnalyticsService.scoreTotal(new BigDecimal("49.999")));
        assertTrue(LecturerAnalyticsService.isAtRiskScore(
                LecturerAnalyticsService.scoreTotal(new BigDecimal("49.999"))));
        assertEquals(new BigDecimal("50.00"), LecturerAnalyticsService.scoreTotal(new BigDecimal("50.009")));
        assertFalse(LecturerAnalyticsService.isAtRiskScore(
                LecturerAnalyticsService.scoreTotal(new BigDecimal("50.009"))));
    }
}
