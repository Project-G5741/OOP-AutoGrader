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

    public long countActiveStudents() {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                FROM user_account u
                JOIN user_role ur ON ur.user_id = u.id
                JOIN role r ON r.id = ur.role_id
                WHERE u.is_active = true AND LOWER(r.name) = 'student'
                """;
        return singleLong(sql, Map.of());
    }

    public long countLabs() {
        String sql = "SELECT COUNT(*) FROM lab";
        return singleLong(sql, Map.of());
    }

    public Object findAverageScore() {
        String sql = "SELECT AVG(p.highest_score) FROM student_lab_progress p";
        List<?> result = entityManager.createNativeQuery(sql).getResultList();
        if (result.isEmpty() || result.get(0) == null) {
            return null;
        }
        return result.get(0);
    }

    public long countAtRiskStudents() {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT u.id
                    FROM user_account u
                    JOIN user_role ur ON ur.user_id = u.id
                    JOIN role r ON r.id = ur.role_id
                    JOIN student_lab_progress p ON p.user_id = u.id
                    WHERE u.is_active = true AND LOWER(r.name) = 'student'
                    GROUP BY u.id
                    HAVING AVG(p.highest_score) < 70
                ) at_risk
                """;
        return singleLong(sql, Map.of());
    }

    public long countActiveStudentsWithSubmissions() {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                FROM user_account u
                JOIN user_role ur ON ur.user_id = u.id
                JOIN role r ON r.id = ur.role_id
                JOIN student_lab_progress p ON p.user_id = u.id
                WHERE u.is_active = true
                  AND LOWER(r.name) = 'student'
                  AND p.last_submitted_at IS NOT NULL
                """;
        return singleLong(sql, Map.of());
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
                      WHERE s.user_id = u.id AND s.lab_id = l.id
                  )
                """;
        return singleLong(sql, Map.of("labId", labId));
    }

    public long countStudentsSubmittedForLab(UUID labId) {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                """ + ROSTER_STUDENT_BASE + """
                AND EXISTS (
                    SELECT 1
                    FROM lab_submission s
                    WHERE s.user_id = u.id AND s.lab_id = l.id
                )
                """;
        return singleLong(sql, Map.of("labId", labId));
    }

    public Object[] findLabStatisticsSummary(UUID labId) {
        String sql = """
                SELECT l.id,
                       l.name,
                       AVG(p.highest_score),
                       MAX(p.highest_score),
                       MIN(p.highest_score),
                       (SELECT COUNT(*) FROM lab_submission s WHERE s.lab_id = l.id)
                FROM lab l
                LEFT JOIN student_lab_progress p ON p.lab_id = l.id
                WHERE l.id = :labId
                GROUP BY l.id, l.name
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
                SELECT CASE
                         WHEN p.highest_score < 50 THEN '0-49'
                         WHEN p.highest_score < 70 THEN '50-69'
                         WHEN p.highest_score < 85 THEN '70-84'
                         ELSE '85-100'
                       END AS score_range,
                       COUNT(*) AS bucket_count
                FROM student_lab_progress p
                WHERE p.lab_id = :labId
                GROUP BY score_range
                ORDER BY score_range
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        return query.getResultList();
    }

    public List<Object[]> findLabStudentRoster(UUID labId, String sortColumn, String sortDirection, int offset, int pageSize) {
        String sql = """
                SELECT u.id,
                       u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       latest_sub.score,
                       COALESCE(latest_sub.attempt_number, 0),
                       latest_sub.submitted_at,
                       (p.best_submission_id IS NOT NULL AND p.best_submission_id = latest_sub.id) AS best_submission,
                       latest_sub.id AS submission_id
                """ + ROSTER_STUDENT_BASE + """
                LEFT JOIN student_lab_progress p ON p.user_id = u.id AND p.lab_id = l.id
                LEFT JOIN LATERAL (
                    SELECT s.id, s.score, s.attempt_number, s.submitted_at
                    FROM lab_submission s
                    WHERE s.user_id = u.id AND s.lab_id = l.id
                    ORDER BY s.attempt_number DESC
                    LIMIT 1
                ) latest_sub ON true
                ORDER BY %s %s
                LIMIT :pageSize OFFSET :offset
                """.formatted(sortColumn, sortDirection);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
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
                      AND (
                          scr.id IS NOT NULL
                          OR EXISTS (
                              SELECT 1
                              FROM submission_field_result sfr
                              JOIN field f ON f.id = sfr.field_id
                              JOIN class_entity ce ON ce.id = f.class_id
                              WHERE sfr.submission_id = s.id AND ce.challenge_id = :challengeId
                          )
                          OR EXISTS (
                              SELECT 1
                              FROM submission_method_result smr
                              JOIN method m ON m.id = smr.method_id
                              JOIN class_entity ce ON ce.id = m.class_id
                              WHERE smr.submission_id = s.id AND ce.challenge_id = :challengeId
                          )
                          OR EXISTS (
                              SELECT 1
                              FROM submission_constructor_result scr_c
                              JOIN "constructor" c ON c.id = scr_c.constructor_id
                              JOIN class_entity ce ON ce.id = c.class_id
                              WHERE scr_c.submission_id = s.id AND ce.challenge_id = :challengeId
                          )
                      )
                    ORDER BY s.attempt_number DESC
                    LIMIT 1
                ) challenge_sub ON true
                LEFT JOIN LATERAL (
                    SELECT COUNT(DISTINCT s2.id) AS attempt_count
                    FROM lab_submission s2
                    WHERE s2.user_id = u.id AND s2.lab_id = l.id
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
                    SELECT p.user_id FROM student_lab_progress p
                    UNION
                    SELECT te.user_id
                    FROM term_enrollment te
                    JOIN lab l ON l.term_id = te.term_id
                    UNION
                    SELECT DISTINCT s.user_id FROM lab_submission s
                )
                """;
        return singleLong(sql, Map.of());
    }

    public List<Object[]> findGradeOverviewStudents(int offset, int pageSize) {
        String sql = """
                SELECT u.id, u.full_name, COALESCE(u.student_code, u.teacher_code)
                FROM user_account u
                WHERE u.id IN (
                    SELECT p.user_id FROM student_lab_progress p
                    UNION
                    SELECT te.user_id
                    FROM term_enrollment te
                    JOIN lab l ON l.term_id = te.term_id
                    UNION
                    SELECT DISTINCT s.user_id FROM lab_submission s
                )
                ORDER BY u.full_name
                LIMIT :pageSize OFFSET :offset
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public List<Object[]> findLabScoresForStudents(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT s.user_id, s.lab_id, s.score
                FROM lab_submission s
                INNER JOIN (
                    SELECT ls.user_id, ls.lab_id, MAX(ls.attempt_number) AS max_attempt
                    FROM lab_submission ls
                    WHERE ls.user_id IN (:studentIds)
                    GROUP BY ls.user_id, ls.lab_id
                ) latest ON latest.user_id = s.user_id
                    AND latest.lab_id = s.lab_id
                    AND latest.max_attempt = s.attempt_number
                WHERE s.user_id IN (:studentIds)
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
