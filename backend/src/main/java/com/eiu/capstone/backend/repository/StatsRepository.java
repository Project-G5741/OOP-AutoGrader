package com.eiu.capstone.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<StatsRow> findStats(UUID studentId, UUID labId) {
        String sql = """
                SELECT p.attempts_count,
                       latest_sub.score,
                       latest_sub.submitted_at,
                       COALESCE((
                           SELECT COUNT(*)
                           FROM lab_submission ls
                           WHERE ls.user_id = :studentId AND ls.lab_id = :labId
                       ), 0) AS submission_count
                FROM student_lab_progress p
                LEFT JOIN LATERAL (
                    SELECT s.score, s.submitted_at
                    FROM lab_submission s
                    WHERE s.user_id = :studentId AND s.lab_id = :labId
                    ORDER BY s.attempt_number DESC
                    LIMIT 1
                ) latest_sub ON true
                WHERE p.user_id = :studentId AND p.lab_id = :labId
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("studentId", studentId)
                .setParameter("labId", labId)
                .getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = rows.get(0);
        Integer attemptsFromProgress = row[0] == null ? null : ((Number) row[0]).intValue();
        BigDecimal latestScore = row[1] == null ? null : new BigDecimal(row[1].toString());
        Object submittedAt = row[2];
        int submissionCount = row[3] == null ? 0 : ((Number) row[3]).intValue();

        return Optional.of(new StatsRow(attemptsFromProgress, latestScore, submittedAt, submissionCount));
    }

    public record StatsRow(
            Integer attemptsFromProgress,
            BigDecimal latestScore,
            Object latestSubmittedAt,
            int submissionCount) {

        public OffsetDateTime latestSubmittedAtOffset() {
            if (latestSubmittedAt instanceof OffsetDateTime offsetDateTime) {
                return offsetDateTime;
            }
            return null;
        }

        public LocalDateTime latestSubmittedAtLocal() {
            if (latestSubmittedAt instanceof LocalDateTime localDateTime) {
                return localDateTime;
            }
            return null;
        }
    }
}
