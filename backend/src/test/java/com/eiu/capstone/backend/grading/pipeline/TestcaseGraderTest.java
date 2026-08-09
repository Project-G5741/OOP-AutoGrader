package com.eiu.capstone.backend.grading.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;

class TestcaseGraderTest {

    @Test
    void emptyTestcaseRowsScoreZeroPercent() {
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(),
                List.of());

        ChallengeGradingContext context = ChallengeGradingContext.of(rubric, null, null, List.of());
        TestcaseGrader grader = new TestcaseGrader();

        TestcaseGrader.TestcasePillarResult result = grader.grade(context);

        assertEquals(0, result.pillarPercentage().compareTo(BigDecimal.ZERO));
        assertTrue(result.results().isEmpty());
    }
}
