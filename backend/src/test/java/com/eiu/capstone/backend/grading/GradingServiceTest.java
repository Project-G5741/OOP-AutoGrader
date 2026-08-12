package com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.scoring.PillarScoreAggregator;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

class GradingServiceTest {

    @Test
    void challengePercentageUsesThreeEqualPillars() {
        BigDecimal result = PillarScoreAggregator.challengePercentage(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                true,
                BigDecimal.ZERO,
                true);

        assertEquals(new BigDecimal("66.67"), result);
    }

    @Test
    void testcaseStatusMapsToFrontendPassFailError() {
        assertEquals("PASS", LabResultAssembler.toFrontendResult(TestcaseResultStatus.PASSED));
        assertEquals("FAIL", LabResultAssembler.toFrontendResult(TestcaseResultStatus.FAILED));
        assertEquals("ERROR", LabResultAssembler.toFrontendResult(TestcaseResultStatus.ERROR));
    }
}
