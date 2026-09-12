package com.eiu.capstone.backend.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.eiu.capstone.backend.utility.TimeUtil;

@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<UUID, StatsRow> findStatsByLabIds(UUID studentId, Collection<UUID> labIds) {
        if (studentId == null || labIds == null || labIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT p.lab_id,
                       p.attempts_count,
                       latest_sub.score,
                       COALESCE(latest_sub.submitted_at, p.last_submitted_at) AS latest_submitted_at,
                       COALESCE(counts.submission_count, 0) AS submission_count
                FROM student_lab_progress p
                LEFT JOIN LATERAL (
                    SELECT s.score, s.submitted_at
                    FROM lab_submission s
                    WHERE s.user_id = :studentId AND s.lab_id = p.lab_id
                    ORDER BY s.attempt_number DESC
                    LIMIT 1
                ) latest_sub ON true
                LEFT JOIN (
                    SELECT lab_id, COUNT(*) AS submission_count
                    FROM lab_submission
                    WHERE user_id = :studentId AND lab_id IN (:labIds)
                    GROUP BY lab_id
                ) counts ON counts.lab_id = p.lab_id
                WHERE p.user_id = :studentId AND p.lab_id IN (:labIds)
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("studentId", studentId)
                .setParameter("labIds", labIds)
                .getResultList();

        Map<UUID, StatsRow> byLab = new HashMap<>();
        for (Object[] row : rows) {
            UUID labId = toUuid(row[0]);
            if (labId == null) {
                continue;
            }
            Integer attemptsFromProgress = row[1] == null ? null : ((Number) row[1]).intValue();
            BigDecimal latestScore = row[2] == null ? null : new BigDecimal(row[2].toString());
            Object submittedAt = row[3];
            int submissionCount = row[4] == null ? 0 : ((Number) row[4]).intValue();
            byLab.put(labId, new StatsRow(attemptsFromProgress, latestScore, submittedAt, submissionCount));
        }
        return byLab;
    }

    public Optional<StatsRow> findStats(UUID studentId, UUID labId) {
        String sql = """
                SELECT p.attempts_count,
                       latest_sub.score,
                       COALESCE(latest_sub.submitted_at, p.last_submitted_at) AS latest_submitted_at,
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

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    public record StatsRow(
            Integer attemptsFromProgress,
            BigDecimal latestScore,
            Object latestSubmittedAt,
            int submissionCount) {

        public OffsetDateTime latestSubmittedAtOffset() {
            return toOffsetDateTime(latestSubmittedAt);
        }

        private static OffsetDateTime toOffsetDateTime(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof OffsetDateTime offsetDateTime) {
                return offsetDateTime;
            }
            if (value instanceof LocalDateTime localDateTime) {
                return localDateTime.atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
            }
            if (value instanceof Instant instant) {
                return instant.atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
            }
            if (value instanceof Timestamp timestamp) {
                return timestamp.toInstant().atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
            }
            if (value instanceof java.util.Date date) {
                return date.toInstant().atZone(TimeUtil.VIETNAM_ZONE).toOffsetDateTime();
            }
            return null;
        }
    }
}
