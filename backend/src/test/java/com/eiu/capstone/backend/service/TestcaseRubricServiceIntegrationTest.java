package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.ChallengeTestcasesResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.TestcaseType;

@SpringBootTest
class TestcaseRubricServiceIntegrationTest {

    @Autowired private TestcaseRubricService testcaseRubricService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void saveNewTestcase_preservesClientIdInResponse() {
        var row = requireChallengeRow();
        UUID labId = (UUID) row.get("lab_id");
        UUID challengeId = (UUID) row.get("challenge_id");
        UUID constructorId = (UUID) row.get("constructor_id");
        UUID fieldId = (UUID) row.get("field_id");

        UUID testcaseId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = singleFieldPayload(testcaseId, invocationId, constructorId, fieldId);

        ChallengeTestcasesResponse response = testcaseRubricService.saveForChallenge(
                labId, challengeId, List.of(payload));

        TestcaseStructureDTO saved = response.testcases().stream()
                .filter(tc -> testcaseId.equals(tc.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected testcase id " + testcaseId + " in " + response.testcases()));
        assertEquals(invocationId, saved.invocation().id());
        assertEquals(1, saved.assertions().size());
    }

    @Test
    @Transactional
    void saveMultiFieldStateTestcase_persistsThreeAssertions() {
        var row = requireChallengeRow();
        UUID labId = (UUID) row.get("lab_id");
        UUID challengeId = (UUID) row.get("challenge_id");
        UUID constructorId = (UUID) row.get("constructor_id");
        List<UUID> fields = fieldIds(challengeId);
        assumeTrue(fields.size() >= 3, "Need at least 3 fields in challenge");

        UUID testcaseId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        TestcaseStructureDTO payload = new TestcaseStructureDTO(
                testcaseId,
                "integration-multi",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                100,
                false,
                new InvocationStructureDTO(invocationId, InvocationKind.CONSTRUCTOR, constructorId, null,
                        "[2020, \"Toyota\"]", null, "[]"),
                List.of(),
                List.of(
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fields.get(0), "2020", ComparisonMode.EXACT, 0),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fields.get(1), "\"Toyota\"", ComparisonMode.EXACT, 1),
                        new AssertionStructureDTO(UUID.randomUUID(), invocationId, AssertionKind.FIELD_STATE,
                                fields.get(2), "0", ComparisonMode.EXACT, 2)));

        ChallengeTestcasesResponse response = testcaseRubricService.saveForChallenge(
                labId, challengeId, List.of(payload));

        TestcaseStructureDTO saved = response.testcases().stream()
                .filter(tc -> testcaseId.equals(tc.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, saved.assertions().size());
    }

    private java.util.Map<String, Object> requireChallengeRow() {
        return jdbcTemplate.queryForMap("""
                SELECT l.id AS lab_id,
                       c.id AS challenge_id,
                       ctor.id AS constructor_id,
                       f.id AS field_id
                FROM lab l
                JOIN challenge c ON c.lab_id = l.id
                JOIN class_entity ce ON ce.challenge_id = c.id
                JOIN constructor ctor ON ctor.class_id = ce.id
                JOIN field f ON f.class_id = ce.id
                LIMIT 1
                """);
    }

    private List<UUID> fieldIds(UUID challengeId) {
        return jdbcTemplate.queryForList("""
                SELECT f.id FROM field f
                JOIN class_entity ce ON ce.id = f.class_id
                WHERE ce.challenge_id = ?
                ORDER BY f.name
                """, UUID.class, challengeId);
    }

    private TestcaseStructureDTO singleFieldPayload(UUID testcaseId,
                                                    UUID invocationId,
                                                    UUID constructorId,
                                                    UUID fieldId) {
        return new TestcaseStructureDTO(
                testcaseId,
                "integration-save",
                TestcaseType.SINGLE_INVOCATION,
                null,
                1,
                99,
                false,
                new InvocationStructureDTO(
                        invocationId,
                        InvocationKind.CONSTRUCTOR,
                        constructorId,
                        null,
                        "[2020, \"Toyota\"]",
                        null,
                        "[]"),
                List.of(),
                List.of(new AssertionStructureDTO(
                        UUID.randomUUID(),
                        invocationId,
                        AssertionKind.FIELD_STATE,
                        fieldId,
                        "2020",
                        ComparisonMode.EXACT,
                        0)));
    }
}
