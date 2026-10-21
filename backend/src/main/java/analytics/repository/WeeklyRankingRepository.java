package com.eiu.capstone.backend.analytics.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

@Repository
public class WeeklyRankingRepository {

    public record RankSource(
            UUID userId,
            String fullName,
            String irn,
            BigDecimal total,
            Instant tieAt) {
    }

    private static final String CURRENT_TERM_ACTIVE_STUDENT_IDS = """
            SELECT te.user_id
            FROM term_enrollment te
            JOIN user_account u ON u.id = te.user_id
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE te.term_id = :termId
              AND u.is_active = true
              AND LOWER(r.name) = 'student'
            """;

    private static final String ON_TIME_AND_AS_OF = """
            s.submitted_at <= :asOf
            AND (l.deadline_date IS NULL
                 OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<RankSource> findLiveBoardSources(UUID termId, Instant asOf) {
        String sql = """
                WITH visible_labs AS (
                    SELECT l.id FROM lab l
                    WHERE l.term_id = :termId AND l.student_visible = true
                ),
                roster AS (
                    SELECT u.id, u.full_name, u.student_code
                    FROM user_account u
                    WHERE u.id IN (
                """ + CURRENT_TERM_ACTIVE_STUDENT_IDS + """
                    )
                ),
                qualifying AS (
                    SELECT s.user_id, s.lab_id, MAX(s.score) AS score
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    JOIN visible_labs v ON v.id = l.id
                    WHERE
                """ + ON_TIME_AND_AS_OF + """
                    GROUP BY s.user_id, s.lab_id
                ),
                lab_high_time AS (
                    SELECT s.user_id, s.lab_id, MIN(s.submitted_at) AS reached_at
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    JOIN visible_labs v ON v.id = l.id
                    JOIN qualifying q ON q.user_id = s.user_id AND q.lab_id = s.lab_id AND s.score = q.score
                    WHERE
                """ + ON_TIME_AND_AS_OF + """
                    GROUP BY s.user_id, s.lab_id
                )
                SELECT r.id, r.full_name, r.student_code,
                       COALESCE((
                           SELECT SUM(COALESCE(q.score, 0))
                           FROM visible_labs vl
                           LEFT JOIN qualifying q ON q.lab_id = vl.id AND q.user_id = r.id
                       ), 0) AS total,
                       (SELECT MAX(t.reached_at) FROM lab_high_time t WHERE t.user_id = r.id) AS tie_at
                FROM roster r
                ORDER BY total DESC, tie_at ASC NULLS LAST, r.student_code ASC NULLS LAST, r.full_name ASC
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("termId", termId);
        query.setParameter("asOf", Timestamp.from(asOf));
        List<Object[]> rows = query.getResultList();
        List<RankSource> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            out.add(new RankSource(
                    toUuid(row[0]),
                    row[1] == null ? "" : row[1].toString(),
                    row[2] == null ? "" : row[2].toString(),
                    toDecimal(row[3]),
                    toInstant(row[4])));
        }
        return out;
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        return Instant.parse(value.toString());
    }
}
