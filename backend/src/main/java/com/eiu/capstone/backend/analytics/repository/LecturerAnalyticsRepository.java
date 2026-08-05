package com.eiu.capstone.backend.analytics.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

@Repository
public class LecturerAnalyticsRepository {

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

    public Object[] findLabStatisticsSummary(UUID labId) {
        String sql = """
                SELECT l.id,
                       l.name,
                       AVG(p.highest_score),
                       MAX(p.highest_score),
                       MIN(p.highest_score),
                       (SELECT COUNT(*) FROM lab_submission s WHERE s.lab_id = l.id),
                       COUNT(DISTINCT p.user_id)
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

    public long countTotalActiveStudents() {
        return countActiveStudents();
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

    public List<Object[]> findLabSubmissions(UUID labId, String sortColumn, String sortDirection, int offset, int pageSize) {
        String sql = """
                SELECT u.full_name,
                       COALESCE(u.student_code, u.teacher_code),
                       s.score,
                       s.attempt_number,
                       s.submitted_at,
                       (p.best_submission_id = s.id) AS best_submission
                FROM lab_submission s
                JOIN user_account u ON s.user_id = u.id
                LEFT JOIN student_lab_progress p ON p.user_id = s.user_id AND p.lab_id = s.lab_id
                WHERE s.lab_id = :labId
                ORDER BY %s %s
                LIMIT :pageSize OFFSET :offset
                """.formatted(sortColumn, sortDirection);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("labId", labId);
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public long countLabSubmissions(UUID labId) {
        String sql = "SELECT COUNT(*) FROM lab_submission s WHERE s.lab_id = :labId";
        return singleLong(sql, Map.of("labId", labId));
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
