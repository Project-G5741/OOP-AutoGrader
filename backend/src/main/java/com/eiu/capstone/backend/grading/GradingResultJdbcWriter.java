package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.AssertionRow;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.MemberFlag;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.TestcaseRow;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

@Component
public class GradingResultJdbcWriter {

    private final DataSource dataSource;

    public GradingResultJdbcWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void upsertChallengeResults(List<SubmissionChallengeResult> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        UUID submissionId = rows.get(0).getSubmission().getId();
        UUID[] challengeIds = new UUID[rows.size()];
        Boolean[] correct = new Boolean[rows.size()];
        BigDecimal[] scores = new BigDecimal[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            SubmissionChallengeResult row = rows.get(i);
            challengeIds[i] = row.getChallenge().getId();
            correct[i] = row.isCorrect();
            scores[i] = row.getScore() == null ? BigDecimal.ZERO : row.getScore();
        }
        String sql = """
                INSERT INTO submission_challenge_result (id, submission_id, challenge_id, is_correct, score)
                SELECT gen_random_uuid(), ?, x.challenge_id, x.is_correct, x.score
                FROM unnest(?::uuid[], ?::boolean[], ?::numeric[])
                    AS x(challenge_id, is_correct, score)
                ON CONFLICT ON CONSTRAINT submission_challenge_result_key
                DO UPDATE SET is_correct = EXCLUDED.is_correct, score = EXCLUDED.score
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, submissionId);
            statement.setArray(2, connection.createArrayOf("uuid", challengeIds));
            statement.setArray(3, connection.createArrayOf("bool", correct));
            statement.setArray(4, connection.createArrayOf("numeric", scores));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert challenge results", e);
        }
    }

    public void upsertDetails(GradingDetailPersistPayload payload) {
        if (payload == null || payload.submissionId() == null) {
            return;
        }
        UUID submissionId = payload.submissionId();
        upsertFlags(submissionId, "submission_field_result", "field_id", "submission_field_result_key",
                payload.fields());
        upsertFlags(submissionId, "submission_method_result", "method_id", "submission_method_result_key",
                payload.methods());
        upsertFlags(submissionId, "submission_constructor_result", "constructor_id",
                "submission_constructor_result_key", payload.constructors());
        upsertFlags(submissionId, "submission_relation_result", "class_relation_id",
                "submission_relation_result_key", payload.relations());
        upsertTestcases(submissionId, payload.testcases());
    }

    private void upsertFlags(UUID submissionId, String table, String elementColumn, String constraint,
                             List<MemberFlag> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        UUID[] ids = new UUID[rows.size()];
        Boolean[] correct = new Boolean[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            ids[i] = rows.get(i).elementId();
            correct[i] = rows.get(i).correct();
        }
        String sql = """
                INSERT INTO %s (id, submission_id, %s, is_correct)
                SELECT gen_random_uuid(), ?, x.element_id, x.is_correct
                FROM unnest(?::uuid[], ?::boolean[]) AS x(element_id, is_correct)
                ON CONFLICT ON CONSTRAINT %s
                DO UPDATE SET is_correct = EXCLUDED.is_correct
                """.formatted(table, elementColumn, constraint);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, submissionId);
            statement.setArray(2, connection.createArrayOf("uuid", ids));
            statement.setArray(3, connection.createArrayOf("bool", correct));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert " + table, e);
        }
    }

    private void upsertTestcases(UUID submissionId, List<TestcaseRow> testcases) {
        if (testcases == null || testcases.isEmpty()) {
            return;
        }
        UUID[] testcaseIds = new UUID[testcases.size()];
        String[] results = new String[testcases.size()];
        String[] feedback = new String[testcases.size()];
        String[] input = new String[testcases.size()];
        String[] expected = new String[testcases.size()];
        String[] actual = new String[testcases.size()];
        for (int i = 0; i < testcases.size(); i++) {
            TestcaseRow row = testcases.get(i);
            testcaseIds[i] = row.testcaseId();
            results[i] = statusName(row.status());
            feedback[i] = row.feedback();
            input[i] = row.inputDisplay();
            expected[i] = row.expectedDisplay();
            actual[i] = row.actualDisplay();
        }
        String sql = """
                INSERT INTO submission_testcase_result (
                    id, submission_id, testcase_id, result, feedback,
                    input_display, expected_display, actual_display, created_at)
                SELECT gen_random_uuid(), ?, x.testcase_id, x.result::testcase_result_status,
                       x.feedback, x.input_display, x.expected_display, x.actual_display, now()
                FROM unnest(?::uuid[], ?::text[], ?::text[], ?::text[], ?::text[], ?::text[])
                    AS x(testcase_id, result, feedback, input_display, expected_display, actual_display)
                ON CONFLICT ON CONSTRAINT submission_testcase_result_key
                DO UPDATE SET
                    result = EXCLUDED.result,
                    feedback = EXCLUDED.feedback,
                    input_display = EXCLUDED.input_display,
                    expected_display = EXCLUDED.expected_display,
                    actual_display = EXCLUDED.actual_display
                RETURNING id, testcase_id
                """;
        Map<UUID, UUID> resultIdByTestcaseId = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, submissionId);
            statement.setArray(2, uuidArray(connection, testcaseIds));
            statement.setArray(3, connection.createArrayOf("text", results));
            statement.setArray(4, connection.createArrayOf("text", feedback));
            statement.setArray(5, connection.createArrayOf("text", input));
            statement.setArray(6, connection.createArrayOf("text", expected));
            statement.setArray(7, connection.createArrayOf("text", actual));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    resultIdByTestcaseId.put(rs.getObject("testcase_id", UUID.class),
                            rs.getObject("id", UUID.class));
                }
            }
            upsertAssertions(connection, testcases, resultIdByTestcaseId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert testcase results", e);
        }
    }

    private void upsertAssertions(Connection connection, List<TestcaseRow> testcases,
                                  Map<UUID, UUID> resultIdByTestcaseId) throws SQLException {
        List<UUID> parentIds = new ArrayList<>();
        List<UUID> assertionIds = new ArrayList<>();
        List<String> results = new ArrayList<>();
        List<String> actuals = new ArrayList<>();
        List<String> feedback = new ArrayList<>();
        for (TestcaseRow testcase : testcases) {
            UUID parentId = resultIdByTestcaseId.get(testcase.testcaseId());
            if (parentId == null || testcase.assertions() == null) {
                continue;
            }
            for (AssertionRow assertion : testcase.assertions()) {
                parentIds.add(parentId);
                assertionIds.add(assertion.assertionId());
                results.add(statusName(assertion.status()));
                actuals.add(assertion.actualValueJson());
                feedback.add(assertion.feedback());
            }
        }
        if (parentIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO submission_testcase_assertion_result (
                    id, submission_testcase_result_id, testcase_assertion_id,
                    result, actual_value, feedback, created_at)
                SELECT gen_random_uuid(), x.parent_id, x.assertion_id, x.result::testcase_result_status,
                       CAST(x.actual_value AS jsonb), x.feedback, now()
                FROM unnest(?::uuid[], ?::uuid[], ?::text[], ?::text[], ?::text[])
                    AS x(parent_id, assertion_id, result, actual_value, feedback)
                ON CONFLICT ON CONSTRAINT submission_testcase_assertion_result_key
                DO UPDATE SET
                    result = EXCLUDED.result,
                    actual_value = EXCLUDED.actual_value,
                    feedback = EXCLUDED.feedback
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setArray(1, uuidArray(connection, parentIds.toArray(UUID[]::new)));
            statement.setArray(2, uuidArray(connection, assertionIds.toArray(UUID[]::new)));
            statement.setArray(3, connection.createArrayOf("text", results.toArray()));
            statement.setArray(4, connection.createArrayOf("text", actuals.toArray()));
            statement.setArray(5, connection.createArrayOf("text", feedback.toArray()));
            statement.executeUpdate();
        }
    }

    private static Array uuidArray(Connection connection, UUID[] values) throws SQLException {
        return connection.createArrayOf("uuid", values);
    }

    private static String statusName(TestcaseResultStatus status) {
        return status == null ? TestcaseResultStatus.ERROR.name() : status.name();
    }
}
