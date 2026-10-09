package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.AssertionRow;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.MemberFlag;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.TestcaseRow;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;

import static com.eiu.capstone.backend.grading.GradingResultJdbcWriterSupport.isUniqueViolation;
import static com.eiu.capstone.backend.grading.GradingResultJdbcWriterSupport.statusName;
import static com.eiu.capstone.backend.grading.GradingResultJdbcWriterSupport.withConnection;
import static com.eiu.capstone.backend.grading.GradingResultJdbcWriterSupport.withTransaction;

@Component
@Profile("desktop")
public class H2GradingResultJdbcWriter implements GradingResultJdbcWriter {

    private final DataSource dataSource;

    public H2GradingResultJdbcWriter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public UploadWriteResult persistUpload(UUID submissionId,
                                           UUID userId,
                                           UUID labId,
                                           BigDecimal score,
                                           OffsetDateTime submittedAt,
                                           List<SubmissionChallengeResult> challengeRows,
                                           String desktopPackVersion) {
        IllegalStateException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return persistUploadOnce(
                        submissionId, userId, labId, score, submittedAt, challengeRows, desktopPackVersion);
            } catch (IllegalStateException e) {
                if (attempt == 0 && isUniqueViolation(e)) {
                    last = e;
                    continue;
                }
                throw e;
            }
        }
        throw new IllegalStateException("Failed to persist upload", last);
    }

    private UploadWriteResult persistUploadOnce(UUID submissionId,
                                                UUID userId,
                                                UUID labId,
                                                BigDecimal score,
                                                OffsetDateTime submittedAt,
                                                List<SubmissionChallengeResult> challengeRows,
                                                String desktopPackVersion) {
        BigDecimal persistedScore = score == null ? BigDecimal.ZERO : score;
        UploadWriteResult[] result = new UploadWriteResult[1];
        withConnection(dataSource, "Failed to persist upload", connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int attemptNumber = nextAttemptNumber(connection, userId, labId);
                insertSubmission(connection, submissionId, userId, labId, attemptNumber, persistedScore, submittedAt,
                        desktopPackVersion);
                insertChallengeResults(connection, submissionId, challengeRows);
                OffsetDateTime lastSubmittedAt =
                        upsertProgress(connection, userId, labId, submissionId, persistedScore, attemptNumber,
                                submittedAt);
                connection.commit();
                result[0] = new UploadWriteResult(attemptNumber, lastSubmittedAt);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
        return result[0];
    }

    private static int nextAttemptNumber(Connection connection, UUID userId, UUID labId) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(attempt_number), 0) + 1 AS next_attempt
                FROM lab_submission WHERE user_id = ? AND lab_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, labId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt("next_attempt");
            }
        }
    }

    private static void insertSubmission(Connection connection,
                                         UUID submissionId,
                                         UUID userId,
                                         UUID labId,
                                         int attemptNumber,
                                         BigDecimal score,
                                         OffsetDateTime submittedAt,
                                         String desktopPackVersion) throws SQLException {
        String sql = """
                INSERT INTO lab_submission (id, user_id, lab_id, attempt_number, score, submitted_at, desktop_pack_version)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, submissionId);
            statement.setObject(2, userId);
            statement.setObject(3, labId);
            statement.setInt(4, attemptNumber);
            statement.setBigDecimal(5, score);
            statement.setObject(6, submittedAt);
            statement.setString(7, desktopPackVersion);
            statement.executeUpdate();
        }
    }

    private static void insertChallengeResults(Connection connection,
                                               UUID submissionId,
                                               List<SubmissionChallengeResult> challengeRows) throws SQLException {
        if (challengeRows == null || challengeRows.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO submission_challenge_result (id, submission_id, challenge_id, is_correct, score)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SubmissionChallengeResult row : challengeRows) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, submissionId);
                statement.setObject(3, row.getChallenge().getId());
                statement.setBoolean(4, row.isCorrect());
                statement.setBigDecimal(5, row.getScore() == null ? BigDecimal.ZERO : row.getScore());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static OffsetDateTime upsertProgress(Connection connection,
                                                 UUID userId,
                                                 UUID labId,
                                                 UUID submissionId,
                                                 BigDecimal score,
                                                 int attemptNumber,
                                                 OffsetDateTime submittedAt) throws SQLException {
        ProgressSnapshot existing = loadProgress(connection, userId, labId);
        if (existing == null) {
            String insert = """
                    INSERT INTO student_lab_progress (
                        id, user_id, lab_id, highest_score, attempts_count, best_submission_id,
                        first_submitted_at, last_submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, userId);
                statement.setObject(3, labId);
                statement.setBigDecimal(4, score);
                statement.setInt(5, attemptNumber);
                statement.setObject(6, submissionId);
                statement.setObject(7, submittedAt);
                statement.setObject(8, submittedAt);
                statement.executeUpdate();
            }
            return submittedAt;
        }

        BigDecimal newHighest = existing.highestScore().max(score);
        UUID bestSubmissionId = score.compareTo(existing.highestScore()) > 0
                ? submissionId
                : existing.bestSubmissionId();
        OffsetDateTime firstSubmitted = existing.firstSubmittedAt() == null ? submittedAt : existing.firstSubmittedAt();

        String update = """
                UPDATE student_lab_progress SET
                    last_submitted_at = ?,
                    attempts_count = ?,
                    highest_score = ?,
                    best_submission_id = ?,
                    first_submitted_at = ?
                WHERE user_id = ? AND lab_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setObject(1, submittedAt);
            statement.setInt(2, attemptNumber);
            statement.setBigDecimal(3, newHighest);
            statement.setObject(4, bestSubmissionId);
            statement.setObject(5, firstSubmitted);
            statement.setObject(6, userId);
            statement.setObject(7, labId);
            statement.executeUpdate();
        }
        return submittedAt;
    }

    private record ProgressSnapshot(
            BigDecimal highestScore,
            UUID bestSubmissionId,
            OffsetDateTime firstSubmittedAt) {}

    private static ProgressSnapshot loadProgress(Connection connection, UUID userId, UUID labId) throws SQLException {
        String sql = """
                SELECT highest_score, best_submission_id, first_submitted_at
                FROM student_lab_progress WHERE user_id = ? AND lab_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, labId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                BigDecimal highest = rs.getBigDecimal("highest_score");
                if (highest == null) {
                    highest = BigDecimal.ZERO;
                }
                return new ProgressSnapshot(
                        highest,
                        rs.getObject("best_submission_id", UUID.class),
                        rs.getObject("first_submitted_at", OffsetDateTime.class));
            }
        }
    }

    @Override
    public void upsertChallengeResults(List<SubmissionChallengeResult> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        UUID submissionId = rows.get(0).getSubmission().getId();
        String merge = """
                MERGE INTO submission_challenge_result (id, submission_id, challenge_id, is_correct, score)
                KEY (submission_id, challenge_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        withConnection(dataSource, "Failed to upsert challenge results", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(merge)) {
                for (SubmissionChallengeResult row : rows) {
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, submissionId);
                    statement.setObject(3, row.getChallenge().getId());
                    statement.setBoolean(4, row.isCorrect());
                    statement.setBigDecimal(5, row.getScore() == null ? BigDecimal.ZERO : row.getScore());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void upsertDetails(GradingDetailPersistPayload payload) {
        if (payload == null || payload.submissionId() == null) {
            return;
        }
        if (isEmpty(payload.fields()) && isEmpty(payload.methods()) && isEmpty(payload.constructors())
                && isEmpty(payload.relations()) && isEmpty(payload.testcases())) {
            return;
        }
        UUID submissionId = payload.submissionId();
        withTransaction(dataSource, "Failed to upsert submission details", connection -> {
            mergeFlags(connection, submissionId, "submission_field_result", "field_id", payload.fields());
            mergeFlags(connection, submissionId, "submission_method_result", "method_id", payload.methods());
            mergeFlags(connection, submissionId, "submission_constructor_result", "constructor_id",
                    payload.constructors());
            mergeFlags(connection, submissionId, "submission_relation_result", "class_relation_id",
                    payload.relations());
            mergeTestcases(connection, submissionId, payload.testcases());
        });
    }

    private static boolean isEmpty(List<?> rows) {
        return rows == null || rows.isEmpty();
    }

    private void mergeFlags(Connection connection, UUID submissionId, String table, String elementColumn,
                            List<MemberFlag> rows) throws SQLException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        String sql = """
                MERGE INTO %s (id, submission_id, %s, is_correct)
                KEY (submission_id, %s)
                VALUES (?, ?, ?, ?)
                """.formatted(table, elementColumn, elementColumn);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (MemberFlag row : rows) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, submissionId);
                statement.setObject(3, row.elementId());
                statement.setBoolean(4, row.correct());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void mergeTestcases(Connection connection, UUID submissionId, List<TestcaseRow> testcases)
            throws SQLException {
        if (testcases == null || testcases.isEmpty()) {
            return;
        }
        Map<UUID, UUID> resultIdByTestcaseId = new HashMap<>();
        String merge = """
                MERGE INTO submission_testcase_result (
                    id, submission_id, testcase_id, result, feedback,
                    input_display, expected_display, actual_display, created_at)
                KEY (submission_id, testcase_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(merge)) {
            for (TestcaseRow row : testcases) {
                UUID resultId = UUID.randomUUID();
                resultIdByTestcaseId.put(row.testcaseId(), resultId);
                statement.setObject(1, resultId);
                statement.setObject(2, submissionId);
                statement.setObject(3, row.testcaseId());
                statement.setString(4, statusName(row.status()));
                statement.setString(5, row.feedback());
                statement.setString(6, row.inputDisplay());
                statement.setString(7, row.expectedDisplay());
                statement.setString(8, row.actualDisplay());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        mergeAssertions(connection, testcases, resultIdByTestcaseId);
    }

    private void mergeAssertions(Connection connection,
                                 List<TestcaseRow> testcases,
                                 Map<UUID, UUID> resultIdByTestcaseId) throws SQLException {
        String merge = """
                MERGE INTO submission_testcase_assertion_result (
                    id, submission_testcase_result_id, testcase_assertion_id,
                    result, actual_value, feedback, created_at)
                KEY (submission_testcase_result_id, testcase_assertion_id)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (PreparedStatement statement = connection.prepareStatement(merge)) {
            for (TestcaseRow testcase : testcases) {
                UUID parentId = resultIdByTestcaseId.get(testcase.testcaseId());
                if (parentId == null || testcase.assertions() == null) {
                    continue;
                }
                for (AssertionRow assertion : testcase.assertions()) {
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, parentId);
                    statement.setObject(3, assertion.assertionId());
                    statement.setString(4, statusName(assertion.status()));
                    statement.setString(5, assertion.actualValueJson());
                    statement.setString(6, assertion.feedback());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }
}
