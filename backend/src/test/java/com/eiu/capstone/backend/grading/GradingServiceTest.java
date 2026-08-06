package com.eiu.capstone.backend.grading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradingServiceTest {

    @Test
    void challengePercentageUsesSeparateJavaAndMmdScores() {
        BigDecimal result = GradingService.calculateChallengePercentage(1, 1, 2);

        assertEquals(0, result.compareTo(new BigDecimal("50.00")));
    }
}
