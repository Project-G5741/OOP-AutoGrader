package com.eiu.capstone.backend.grading.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.eiu.capstone.backend.grading.MmdComparisonService;
import com.eiu.capstone.backend.grading.MmdParser;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;

class MmdPillarGraderTest {

    private final MmdPillarGrader grader = new MmdPillarGrader(new MmdParser(), new MmdComparisonService());

    @Test
    void parseFailureExposesParseErrorAndZeroesPillar() {
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Bad",
                List.of(new ClassRubric(classId, "Animal", "public", "CLASS", false, List.of(), List.of(), List.of())),
                List.of(),
                List.of());

        MockMultipartFile badMmd = new MockMultipartFile(
                "files",
                "diagram.mmd",
                "text/plain",
                "classDiagram\nclass Bad Name".getBytes(StandardCharsets.UTF_8));

        MmdPillarGrader.MmdPillarResult result = grader.grade(rubric, List.of(badMmd));

        assertNotNull(result.parseError());
        assertTrue(result.pillarPercentage().doubleValue() == 0.0);
        assertTrue(result.mmdSubmitted());
        assertTrue(!result.outcome().isClassPresent(classId));
    }

    @Test
    void successfulParseLeavesParseErrorNull() {
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Animal",
                List.of(new ClassRubric(classId, "Animal", "public", "CLASS", false, List.of(), List.of(), List.of())),
                List.of(),
                List.of());

        MockMultipartFile mmd = new MockMultipartFile(
                "files",
                "diagram.mmd",
                "text/plain",
                "classDiagram\nclass Animal".getBytes(StandardCharsets.UTF_8));

        MmdPillarGrader.MmdPillarResult result = grader.grade(rubric, List.of(mmd));

        assertNull(result.parseError());
        assertTrue(result.outcome().isClassPresent(classId));
    }
}
