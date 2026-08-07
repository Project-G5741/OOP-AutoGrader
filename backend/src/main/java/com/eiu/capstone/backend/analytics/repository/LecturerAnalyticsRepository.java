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

    private static final String ENROLLED_STUDENT_BASE = """
            FROM term_enrollment te
            JOIN lab l ON l.id = :labId AND l.term_id = te.term_id
            JOIN user_account u ON u.id = te.user_id AND u.is_active = true
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id AND LOWER(r.name) = 'student'
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
                WHERE u.is_active = true
                ORDER BY s.submitted_at DESC
                LIMIT :limit
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", limit);
        return query.getResultList();
    }

    public long countEnrolledStudentsForLab(UUID labId) {
        String sql = "SELECT COUNT(DISTINCT u.id) " + ENROLLED_STUDENT_BASE;
        return singleLong(sql, Map.of("labId", labId));
    }

    public long countStudentsSubmittedForLab(UUID labId) {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                """ + ENROLLED_STUDENT_BASE + """
                JOIN lab_submission s ON s.user_id = u.id AND s.lab_id = l.id
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
                SELECT u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       latest_sub.score,
                       COALESCE(latest_sub.attempt_number, 0),
                       latest_sub.submitted_at,
                       (p.best_submission_id IS NOT NULL AND p.best_submission_id = latest_sub.id) AS best_submission,
                       latest_sub.id AS submission_id
                """ + ENROLLED_STUDENT_BASE + """
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
