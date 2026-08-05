package com.eiu.capstone.backend.analytics.repository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> findLabTrend(UUID labId, UUID semesterId, UUID academicYearId, String course) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id, l.name, AVG(p.highest_score) as avg_score, COUNT(p.id) as submission_count ")
           .append("FROM student_lab_progress p ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("JOIN academic_years ay ON t.academic_year_id = ay.id ")
           .append("WHERE 1=1 ");

        Map<String, Object> params = new HashMap<>();
        if (labId != null) {
            sql.append("AND l.id = :labId ");
            params.put("labId", labId);
        }
        if (semesterId != null) {
            sql.append("AND t.id = :semesterId ");
            params.put("semesterId", semesterId);
        }
        if (academicYearId != null) {
            sql.append("AND ay.id = :academicYearId ");
            params.put("academicYearId", academicYearId);
        }
        if (course != null && !course.isBlank()) {
            sql.append("AND LOWER(l.name) LIKE :course ");
            params.put("course", "%" + course.trim().toLowerCase() + "%");
        }
        sql.append("GROUP BY l.id, l.name ORDER BY l.name");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public Object[] findDashboardSummary(UUID labId, UUID semesterId, UUID academicYearId, String course) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT AVG(p.highest_score) AS overall_avg, ")
           .append("MIN(lab_avg.avg_score) AS lowest_avg_score, ")
           .append("lab_avg.lab_name AS lowest_avg_lab ")
           .append("FROM student_lab_progress p ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("JOIN academic_years ay ON t.academic_year_id = ay.id ")
           .append("LEFT JOIN ( ")
           .append("  SELECT l2.id AS lab_id, l2.name AS lab_name, AVG(p2.highest_score) AS avg_score ")
           .append("  FROM student_lab_progress p2 ")
           .append("  JOIN lab l2 ON p2.lab_id = l2.id ")
           .append("  JOIN term t2 ON l2.term_id = t2.id ")
           .append("  JOIN academic_years ay2 ON t2.academic_year_id = ay2.id ")
           .append("  WHERE 1=1 ");

        if (labId != null) {
            sql.append(" AND l2.id = :labId ");
        }
        if (semesterId != null) {
            sql.append(" AND t2.id = :semesterId ");
        }
        if (academicYearId != null) {
            sql.append(" AND ay2.id = :academicYearId ");
        }
        if (course != null && !course.isBlank()) {
            sql.append(" AND LOWER(l2.name) LIKE :course ");
        }
        sql.append(" GROUP BY l2.id, l2.name ORDER BY avg_score ASC LIMIT 1")
           .append(") lab_avg ON TRUE ")
           .append("WHERE 1=1 ");

        if (labId != null) {
            sql.append(" AND l.id = :labId ");
        }
        if (semesterId != null) {
            sql.append(" AND t.id = :semesterId ");
        }
        if (academicYearId != null) {
            sql.append(" AND ay.id = :academicYearId ");
        }
        if (course != null && !course.isBlank()) {
            sql.append(" AND LOWER(l.name) LIKE :course ");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        if (labId != null) query.setParameter("labId", labId);
        if (semesterId != null) query.setParameter("semesterId", semesterId);
        if (academicYearId != null) query.setParameter("academicYearId", academicYearId);
        if (course != null && !course.isBlank()) query.setParameter("course", "%" + course.trim().toLowerCase() + "%");

        List<?> result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return (Object[]) result.get(0);
    }

    public List<Object[]> findStudentOverview(UUID labId, UUID semesterId, UUID academicYearId, String search, String sortBy, String direction, int offset, int pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.id, u.full_name, COALESCE(u.student_code, u.teacher_code), AVG(p.highest_score) AS avg_score, COUNT(p.id) AS completed_labs ")
           .append("FROM student_lab_progress p ")
           .append("JOIN user_account u ON p.user_id = u.id ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("WHERE u.is_active = true ");

        if (labId != null) {
            sql.append("AND l.id = :labId ");
        }
        if (semesterId != null) {
            sql.append("AND t.id = :semesterId ");
        }
        if (academicYearId != null) {
            sql.append("AND t.academic_year_id = :academicYearId ");
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(u.full_name) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.student_code) LIKE :search OR LOWER(u.teacher_code) LIKE :search) ");
        }
        sql.append("GROUP BY u.id, u.full_name, u.student_code, u.teacher_code ");
        if ("completedLabs".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY completed_labs " + direction + ", u.full_name");
        } else if ("studentName".equalsIgnoreCase(sortBy)) {
            sql.append("ORDER BY u.full_name " + direction);
        } else {
            sql.append("ORDER BY avg_score " + direction + " NULLS LAST, u.full_name");
        }
        sql.append(" LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (labId != null) query.setParameter("labId", labId);
        if (semesterId != null) query.setParameter("semesterId", semesterId);
        if (academicYearId != null) query.setParameter("academicYearId", academicYearId);
        if (search != null && !search.isBlank()) query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        query.setParameter("pageSize", pageSize);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public long countStudentOverview(UUID labId, UUID semesterId, UUID academicYearId, String search) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT u.id) ")
           .append("FROM student_lab_progress p ")
           .append("JOIN user_account u ON p.user_id = u.id ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("WHERE u.is_active = true ");

        if (labId != null) {
            sql.append("AND l.id = :labId ");
        }
        if (semesterId != null) {
            sql.append("AND t.id = :semesterId ");
        }
        if (academicYearId != null) {
            sql.append("AND t.academic_year_id = :academicYearId ");
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(u.full_name) LIKE :search OR LOWER(u.email) LIKE :search OR LOWER(u.student_code) LIKE :search OR LOWER(u.teacher_code) LIKE :search) ");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        if (labId != null) query.setParameter("labId", labId);
        if (semesterId != null) query.setParameter("semesterId", semesterId);
        if (academicYearId != null) query.setParameter("academicYearId", academicYearId);
        if (search != null && !search.isBlank()) query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        return ((Number) query.getSingleResult()).longValue();
    }

    public List<Object[]> findStudentGradeTrend(UUID studentId) {
        String sql = "SELECT l.id, l.name, p.highest_score " +
                "FROM student_lab_progress p " +
                "JOIN lab l ON p.lab_id = l.id " +
                "WHERE p.user_id = :studentId " +
                "ORDER BY l.name";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        return query.getResultList();
    }

    public List<Object[]> findStudentChallengeBreakdown(UUID studentId) {
        String sql = "SELECT c.name, SUM(CASE WHEN scr.is_correct THEN 1 ELSE 0 END) AS correct_count, COUNT(scr.id) AS total_count " +
                "FROM submission_challenge_result scr " +
                "JOIN lab_submission s ON scr.submission_id = s.id " +
                "JOIN challenge c ON scr.challenge_id = c.id " +
                "WHERE s.user_id = :studentId " +
                "GROUP BY c.name " +
                "ORDER BY (SUM(CASE WHEN scr.is_correct THEN 1 ELSE 0 END) * 1.0 / COUNT(scr.id)) ASC, c.name";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        return query.getResultList();
    }

    public List<Object[]> findStudentWeakSkills(UUID studentId) {
        String sql = "SELECT c.name, SUM(CASE WHEN scr.is_correct THEN 0 ELSE 1 END) AS failure_count " +
                "FROM submission_challenge_result scr " +
                "JOIN lab_submission s ON scr.submission_id = s.id " +
                "JOIN challenge c ON scr.challenge_id = c.id " +
                "WHERE s.user_id = :studentId " +
                "GROUP BY c.name " +
                "HAVING SUM(CASE WHEN scr.is_correct THEN 0 ELSE 1 END) > 0 " +
                "ORDER BY failure_count DESC, c.name " +
                "LIMIT 5";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        return query.getResultList();
    }

    public List<Object[]> findSubmissionHistory(UUID studentId) {
        String sql = "SELECT l.name, s.attempt_number, s.score, s.submitted_at, p.best_submission_id = s.id AS best_submission " +
                "FROM lab_submission s " +
                "JOIN lab l ON s.lab_id = l.id " +
                "LEFT JOIN student_lab_progress p ON p.user_id = s.user_id AND p.lab_id = s.lab_id " +
                "WHERE s.user_id = :studentId " +
                "ORDER BY s.submitted_at DESC";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        return query.getResultList();
    }

    public Object[] findStudentProfileSummary(UUID studentId) {
        String sql = "SELECT u.full_name, COALESCE(u.student_code, u.teacher_code), u.email, " +
                "AVG(p.highest_score) AS overall_avg, MAX(p.highest_score) AS highest_score, COUNT(p.id) AS completed_labs, " +
                "COUNT(s.id) AS submission_count " +
                "FROM user_account u " +
                "LEFT JOIN student_lab_progress p ON p.user_id = u.id " +
                "LEFT JOIN lab_submission s ON s.user_id = u.id " +
                "WHERE u.id = :studentId " +
                "GROUP BY u.full_name, u.student_code, u.teacher_code, u.email";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        List<?> result = query.getResultList();
        if (result.isEmpty()) return null;
        return (Object[]) result.get(0);
    }

    public List<Object[]> findAtRiskLabs(UUID academicYearId, UUID semesterId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id, l.name, AVG(p.highest_score) AS avg_score, MIN(scr_failure.failure_count) AS failure_count, scr_failure.challenge_name ")
           .append("FROM student_lab_progress p ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("LEFT JOIN ( ")
           .append("  SELECT s.lab_id, c.name AS challenge_name, SUM(CASE WHEN scr.is_correct THEN 0 ELSE 1 END) AS failure_count ")
           .append("  FROM submission_challenge_result scr ")
           .append("  JOIN lab_submission s ON scr.submission_id = s.id ")
           .append("  JOIN challenge c ON scr.challenge_id = c.id ")
           .append("  GROUP BY s.lab_id, c.name ")
           .append(") scr_failure ON scr_failure.lab_id = l.id ")
           .append("WHERE 1=1 ");
        if (academicYearId != null) {
            sql.append("AND t.academic_year_id = :academicYearId ");
        }
        if (semesterId != null) {
            sql.append("AND t.id = :semesterId ");
        }
        sql.append("GROUP BY l.id, l.name, scr_failure.challenge_name ")
           .append("ORDER BY avg_score ASC, failure_count DESC ")
           .append("LIMIT 5");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (academicYearId != null) query.setParameter("academicYearId", academicYearId);
        if (semesterId != null) query.setParameter("semesterId", semesterId);
        return query.getResultList();
    }

    public List<Object[]> findAtRiskStudents(UUID academicYearId, UUID semesterId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.id, u.full_name, AVG(p.highest_score) AS avg_score, COUNT(p.id) AS completed_labs ")
           .append("FROM student_lab_progress p ")
           .append("JOIN user_account u ON p.user_id = u.id ")
           .append("JOIN lab l ON p.lab_id = l.id ")
           .append("JOIN term t ON l.term_id = t.id ")
           .append("WHERE u.is_active = true ")
           .append("GROUP BY u.id, u.full_name ");

        if (academicYearId != null) {
            sql.append("HAVING AVG(p.highest_score) < 70 AND SUM(CASE WHEN t.academic_year_id = :academicYearId THEN 1 ELSE 0 END) >= 0 ");
        } else {
            sql.append("HAVING AVG(p.highest_score) < 70 ");
        }
        sql.append(" ORDER BY avg_score ASC LIMIT 5");

        Query query = entityManager.createNativeQuery(sql.toString());
        if (academicYearId != null) query.setParameter("academicYearId", academicYearId);
        return query.getResultList();
    }
}
