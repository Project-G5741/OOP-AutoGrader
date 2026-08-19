package com.eiu.capstone.backend.analytics.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

@Repository
public class LecturerAnalyticsRepository {

    /**
     * Roster population: distinct students with lab progress, union enrolled students for the term.
     */
    private static final String ROSTER_STUDENT_BASE = """
            FROM (
                SELECT DISTINCT p.user_id
                FROM student_lab_progress p
                WHERE p.lab_id = :labId
                UNION
                SELECT DISTINCT te.user_id
                FROM term_enrollment te
                JOIN lab l_enr ON l_enr.term_id = te.term_id AND l_enr.id = :labId
            ) roster_ids
            JOIN user_account u ON u.id = roster_ids.user_id
            JOIN lab l ON l.id = :labId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String GRADE_OVERVIEW_STUDENT_IDS = """
            SELECT p.user_id FROM student_lab_progress p
            UNION
            SELECT te.user_id
            FROM term_enrollment te
            JOIN lab l ON l.term_id = te.term_id
            UNION
            SELECT DISTINCT s.user_id FROM lab_submission s
            """;

    private static final String QUALIFYING_SCORES_CTE = """
                qualifying_scores AS (
                    SELECT s.user_id, s.lab_id, MAX(s.score) AS score
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    WHERE l.deadline_date IS NULL
                       OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh')
                    GROUP BY s.user_id, s.lab_id
                )""";

    private static final String LAB_DEADLINE_SUBMISSION_FILTER = """
                    (l.deadline_date IS NULL
                     OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                    """;

    /**
     * Count active students whose grade-overview total (average of highest lab scores, missing labs as 0) is below 70.
     */
    private static final String AT_RISK_STUDENT_COUNT = """
            SELECT COUNT(*) FROM (
                WITH lab_total AS (
                    SELECT CAST(COUNT(*) AS numeric) AS lab_count FROM lab
                ),
                roster AS (
                    SELECT DISTINCT u.id
                    FROM user_account u
                    JOIN user_role ur ON ur.user_id = u.id
                    JOIN role r ON r.id = ur.role_id
                    WHERE u.is_active = true
                      AND LOWER(r.name) = 'student'
                      AND u.id IN (
            """ + GRADE_OVERVIEW_STUDENT_IDS + """
                      )
                ),
                """ + QUALIFYING_SCORES_CTE + """
                ,
                student_totals AS (
                    SELECT r.id,
                           CASE WHEN lt.lab_count > 0 THEN
                               (SELECT COALESCE(SUM(COALESCE(qs.score, 0)), 0) / lt.lab_count
                                FROM lab l
                                LEFT JOIN qualifying_scores qs
                                    ON qs.user_id = r.id AND qs.lab_id = l.id)
                           END AS total_score
                    FROM roster r
                    CROSS JOIN lab_total lt
                )
                SELECT id FROM student_totals WHERE total_score < 70
            ) at_risk
            """;

    public Object[] findOverviewMetrics() {
        String sql = """
                SELECT
                    (SELECT COUNT(DISTINCT u.id)
                     FROM user_account u
                     JOIN user_role ur ON ur.user_id = u.id
                     JOIN role r ON r.id = ur.role_id
                     WHERE u.is_active = true AND LOWER(r.name) = 'student') AS active_students,
                    (SELECT COUNT(*) FROM lab) AS total_labs,
                    (SELECT AVG(best.score) FROM (
                        SELECT MAX(s.score) AS score
                        FROM lab_submission s
                        JOIN lab l ON l.id = s.lab_id
                        WHERE l.deadline_date IS NULL
                           OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh')
                        GROUP BY s.user_id, s.lab_id
                    ) best) AS average_score,
                    ("""
                + AT_RISK_STUDENT_COUNT
                + """
                    ) AS at_risk_students,
                    (SELECT COUNT(DISTINCT u.id)
                     FROM user_account u
                     JOIN user_role ur ON ur.user_id = u.id
                     JOIN role r ON r.id = ur.role_id
                     JOIN student_lab_progress p ON p.user_id = u.id
                     WHERE u.is_active = true
                       AND LOWER(r.name) = 'student'
                       AND p.last_submitted_at IS NOT NULL) AS active_submitters
                """;
        Query query = entityManager.createNativeQuery(sql);
        List<?> result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return (Object[]) result.get(0);
    }

    public List<Object[]> findRecentSubmissions(int limit) {
        String sql = """
                SELECT u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       l.name,
                       s.score,
                       s.attempt_number,
                       s.submitted_at
                FROM lab_submission s
                JOIN user_account u ON s.user_id = u.id
                JOIN lab l ON s.lab_id = l.id
                ORDER BY s.submitted_at DESC
                LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);
        return query.getResultList();
    }

    public long countEnrolledStudentsForLab(UUID labId) {
        String sql = "SELECT COUNT(DISTINCT u.id) " + ROSTER_STUDENT_BASE;
        return singleLong(sql, Map.of("labId", labId));
    }

    public long countActiveEnrolledStudentsForLab(UUID labId) {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                FROM term_enrollment te
                JOIN lab l ON l.term_id = te.term_id AND l.id = :labId
                JOIN user_account u ON u.id = te.user_id
                JOIN user_role ur ON ur.user_id = u.id
                JOIN role r ON r.id = ur.role_id
                WHERE u.is_active = true AND LOWER(r.name) = 'student'
                """;
        return singleLong(sql, Map.of("labId", labId));
    }

    public long countActiveStudentsSubmittedForLab(UUID labId) {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                FROM term_enrollment te
                JOIN lab l ON l.term_id = te.term_id AND l.id = :labId
                JOIN user_account u ON u.id = te.user_id
                JOIN user_role ur ON ur.user_id = u.id
                JOIN role r ON r.id = ur.role_id
                WHERE u.is_active = true
                  AND LOWER(r.name) = 'student'
                  AND EXISTS (
                      SELECT 1
                      FROM lab_submission s
                      JOIN lab l ON l.id = s.lab_id
                      WHERE s.user_id = u.id AND s.lab_id = l.id
                        AND (l.deadline_date IS NULL
                             OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                  )
                """;
        return singleLong(sql, Map.of("labId", labId));
    }

    public Object[] findLabStatisticsSummary(UUID labId) {
        String sql = """
                WITH qualifying AS (
                    SELECT s.user_id, MAX(s.score) AS score
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    WHERE s.lab_id = :labId
                      AND (l.deadline_date IS NULL
                           OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                    GROUP BY s.user_id
                )
                SELECT l.id,
                       l.name,
                       stats.avg_score,
                       stats.max_score,
                       stats.min_score,
                       (SELECT COUNT(*) FROM lab_submission s WHERE s.lab_id = l.id)
                FROM lab l
                LEFT JOIN LATERAL (
                    SELECT AVG(q.score) AS avg_score,
                           MAX(q.score) AS max_score,
                           MIN(q.score) AS min_score
                    FROM qualifying q
                ) stats ON true
                WHERE l.id = :labId
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        List<?> result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return (Object[]) result.get(0);
    }

    public List<Object[]> findGradeDistribution(UUID labId) {
        String sql = """
                WITH qualifying AS (
                    SELECT s.user_id, MAX(s.score) AS score
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    WHERE s.lab_id = :labId
                      AND (l.deadline_date IS NULL
                           OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                    GROUP BY s.user_id
                )
                SELECT CASE
                         WHEN q.score < 50 THEN '0-49'
                         WHEN q.score < 70 THEN '50-69'
                         WHEN q.score < 85 THEN '70-84'
                         ELSE '85-100'
                       END AS score_range,
                       COUNT(*) AS bucket_count
                FROM qualifying q
                GROUP BY score_range
                ORDER BY score_range
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        return query.getResultList();
    }

    public List<Object[]> findLabStudentRoster(UUID labId, String sortColumn, String sortDirection, int offset, int pageSize) {
        return findLabStudentRosterInternal(labId, sortColumn, sortDirection, offset, pageSize, null, null);
    }

    public List<Object[]> findLabStudentRosterAfter(UUID labId,
                                                  String sortColumn,
                                                  String sortDirection,
                                                  String afterName,
                                                  UUID afterId,
                                                  int pageSize) {
        return findLabStudentRosterInternal(labId, sortColumn, sortDirection, 0, pageSize, afterName, afterId);
    }

    public List<Object[]> findLabStudentRosterExport(UUID labId, String sortColumn, String sortDirection) {
        return findLabStudentRosterInternal(labId, sortColumn, sortDirection, 0, Integer.MAX_VALUE, null, null);
    }

    private static String formatRosterOrderBy(String sortColumn, String sortDirection) {
        if ("p.highest_score".equals(sortColumn) || "qb.score".equals(sortColumn)) {
            return "qb.score " + sortDirection + " NULLS LAST";
        }
        return sortColumn + " " + sortDirection;
    }

    private List<Object[]> findLabStudentRosterInternal(UUID labId,
                                                        String sortColumn,
                                                        String sortDirection,
                                                        int offset,
                                                        int pageSize,
                                                        String afterName,
                                                        UUID afterId) {
        String keysetClause = "";
        if (afterName != null && afterId != null) {
            keysetClause = """
                    AND (
                        %s > :afterName
                        OR (%s = :afterName AND u.id > :afterId)
                    )
                    """.formatted(sortColumn, sortColumn);
        }

        String sql = """
                WITH qualifying_best AS (
                    SELECT s.user_id, MAX(s.score) AS score
                    FROM lab_submission s
                    JOIN lab l ON l.id = s.lab_id
                    WHERE s.lab_id = :labId
                      AND (l.deadline_date IS NULL
                           OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                    GROUP BY s.user_id
                ),
                latest_sub AS (
                    SELECT DISTINCT ON (s.user_id)
                           s.user_id,
                           s.id,
                           s.score,
                           s.attempt_number,
                           s.submitted_at
                    FROM lab_submission s
                    WHERE s.lab_id = :labId
                    ORDER BY s.user_id, s.attempt_number DESC
                )
                SELECT u.id,
                       u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       qb.score,
                       COALESCE(latest_sub.attempt_number, 0),
                       latest_sub.submitted_at,
                       (p.best_submission_id IS NOT NULL AND p.best_submission_id = latest_sub.id) AS best_submission,
                       latest_sub.id AS submission_id
                """ + ROSTER_STUDENT_BASE + """
                LEFT JOIN student_lab_progress p ON p.user_id = u.id AND p.lab_id = l.id
                LEFT JOIN latest_sub ON latest_sub.user_id = u.id
                LEFT JOIN qualifying_best qb ON qb.user_id = u.id
                WHERE 1=1
                """ + keysetClause + """
                ORDER BY %s
                LIMIT :pageSize OFFSET :offset
                """.formatted(formatRosterOrderBy(sortColumn, sortDirection));
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        if (afterName != null && afterId != null) {
            query.setParameter("afterName", afterName);
            query.setParameter("afterId", afterId);
        }
        return query.getResultList();
    }

    private static final String CHALLENGE_GRADED_SUBMISSION_EXISTS = """
            (
                EXISTS (
                    SELECT 1
                    FROM submission_challenge_result scr_x
                    WHERE scr_x.submission_id = s2.id AND scr_x.challenge_id = :challengeId
                )
                OR EXISTS (
                    SELECT 1
                    FROM submission_field_result sfr
                    JOIN field f ON f.id = sfr.field_id
                    JOIN class_entity ce ON ce.id = f.class_id
                    WHERE sfr.submission_id = s2.id AND ce.challenge_id = :challengeId
                )
                OR EXISTS (
                    SELECT 1
                    FROM submission_method_result smr
                    JOIN method m ON m.id = smr.method_id
                    JOIN class_entity ce ON ce.id = m.class_id
                    WHERE smr.submission_id = s2.id AND ce.challenge_id = :challengeId
                )
                OR EXISTS (
                    SELECT 1
                    FROM submission_constructor_result scr_c
                    JOIN "constructor" c ON c.id = scr_c.constructor_id
                    JOIN class_entity ce ON ce.id = c.class_id
                    WHERE scr_c.submission_id = s2.id AND ce.challenge_id = :challengeId
                )
            )
            """;

    private static String challengeGradedSubmissionExists(String submissionAlias) {
        return CHALLENGE_GRADED_SUBMISSION_EXISTS.replace("s2.", submissionAlias + ".");
    }

    public List<Object[]> findChallengeStudentRoster(UUID labId,
                                                     UUID challengeId,
                                                     String sortColumn,
                                                     String sortDirection,
                                                     int offset,
                                                     int pageSize) {
        String sql = """
                SELECT u.id,
                       u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       challenge_sub.scr_score,
                       COALESCE(challenge_attempts.attempt_count, 0),
                       challenge_sub.submitted_at,
                       (challenge_sub.id IS NOT NULL) AS has_submission,
                       challenge_sub.id AS submission_id
                """ + ROSTER_STUDENT_BASE + """
                LEFT JOIN LATERAL (
                    SELECT s.id, s.submitted_at, scr.score AS scr_score, scr.id AS scr_id
                    FROM lab_submission s
                    LEFT JOIN submission_challenge_result scr
                        ON scr.submission_id = s.id AND scr.challenge_id = :challengeId
                    WHERE s.user_id = u.id AND s.lab_id = l.id
                      AND """ + LAB_DEADLINE_SUBMISSION_FILTER + """
                      AND """ + challengeGradedSubmissionExists("s") + """
                    ORDER BY s.attempt_number DESC
                    LIMIT 1
                ) challenge_sub ON true
                LEFT JOIN LATERAL (
                    SELECT COUNT(DISTINCT s2.id) AS attempt_count
                    FROM lab_submission s2
                    WHERE s2.user_id = u.id AND s2.lab_id = l.id
                      AND """ + LAB_DEADLINE_SUBMISSION_FILTER.replace("s.", "s2.") + """
                      AND """ + CHALLENGE_GRADED_SUBMISSION_EXISTS + """
                ) challenge_attempts ON true
                ORDER BY %s %s
                LIMIT :pageSize OFFSET :offset
                """.formatted(sortColumn, sortDirection);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        query.setParameter("challengeId", challengeId);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public List<Object[]> findLabAttemptHistory(UUID labId, UUID studentId) {
        String sql = """
                SELECT s.attempt_number,
                       s.score,
                       s.submitted_at,
                       s.id,
                       (p.best_submission_id IS NOT NULL AND p.best_submission_id = s.id) AS best_submission
                FROM lab_submission s
                JOIN lab l ON l.id = s.lab_id
                LEFT JOIN student_lab_progress p ON p.user_id = s.user_id AND p.lab_id = s.lab_id
                WHERE s.lab_id = :labId AND s.user_id = :studentId
                ORDER BY s.attempt_number DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        query.setParameter("studentId", studentId);
        return query.getResultList();
    }

    public String findLabName(UUID labId) {
        String sql = "SELECT l.name FROM lab l WHERE l.id = :labId";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        List<?> result = query.getResultList();
        if (result.isEmpty() || result.get(0) == null) {
            return null;
        }
        return result.get(0).toString();
    }

    public List<Object[]> findAllLabsOrdered() {
        String sql = "SELECT l.id, l.name FROM lab l ORDER BY l.name";
        return entityManager.createNativeQuery(sql).getResultList();
    }

    public long countGradeOverviewStudents() {
        String sql = """
                SELECT COUNT(*)
                FROM user_account u
                WHERE u.id IN (
                """ + GRADE_OVERVIEW_STUDENT_IDS + """
                )
                """;
        return singleLong(sql, Map.of());
    }

    public List<Object[]> findGradeOverviewStudents(String sortColumn, String sortDirection, UUID sortLabId, int offset, int pageSize) {
        String sql = """
                WITH grade_students AS (
                    SELECT u.id, u.full_name, COALESCE(u.student_code, u.teacher_code) AS irn
                    FROM user_account u
                    WHERE u.id IN (
                """ + GRADE_OVERVIEW_STUDENT_IDS + """
                    )
                ),
                lab_total AS (
                    SELECT CAST(COUNT(*) AS numeric) AS lab_count FROM lab
                ),
                """ + QUALIFYING_SCORES_CTE + """
                ,
                student_totals AS (
                    SELECT gs.id,
                           gs.full_name,
                           gs.irn,
                           CASE WHEN lt.lab_count > 0 THEN
                               (SELECT COALESCE(SUM(COALESCE(qs.score, 0)), 0) / lt.lab_count
                                FROM lab l
                                LEFT JOIN qualifying_scores qs
                                    ON qs.user_id = gs.id AND qs.lab_id = l.id)
                           END AS total_score
                    FROM grade_students gs
                    CROSS JOIN lab_total lt
                )
                SELECT id, full_name, irn
                FROM student_totals
                ORDER BY %s
                LIMIT :pageSize OFFSET :offset
                """.formatted(formatGradeOverviewOrderBy(sortColumn, sortDirection, sortLabId));
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        if ("lab_score".equals(sortColumn) && sortLabId != null) {
            query.setParameter("sortLabId", sortLabId);
        }
        return query.getResultList();
    }

    private static String formatGradeOverviewOrderBy(String sortColumn, String sortDirection, UUID sortLabId) {
        if ("total_score".equals(sortColumn)) {
            return "total_score " + sortDirection + " NULLS LAST, full_name ASC";
        }
        if ("lab_score".equals(sortColumn) && sortLabId != null) {
            return "(SELECT qs.score FROM qualifying_scores qs "
                    + "WHERE qs.user_id = student_totals.id AND qs.lab_id = :sortLabId) "
                    + sortDirection + " NULLS LAST, full_name ASC";
        }
        return sortColumn + " " + sortDirection + ", full_name ASC";
    }

    public List<Object[]> findLabScoresForStudents(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT s.user_id, s.lab_id, MAX(s.score) AS score
                FROM lab_submission s
                JOIN lab l ON l.id = s.lab_id
                WHERE s.user_id IN (:studentIds)
                  AND (l.deadline_date IS NULL
                       OR s.submitted_at <= ((CAST(l.deadline_date AS timestamp) + TIME '23:59:59') AT TIME ZONE 'Asia/Ho_Chi_Minh'))
                GROUP BY s.user_id, s.lab_id
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentIds", studentIds);
        return query.getResultList();
    }

    private long singleLong(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);
        Object result = query.getSingleResult();
        if (result == null) {
            return 0L;
        }
        return ((Number) result).longValue();
    }
}
