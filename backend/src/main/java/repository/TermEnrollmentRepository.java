package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.TermEnrollment;

public interface TermEnrollmentRepository extends JpaRepository<TermEnrollment, UUID> {
    long countByTerm_Id(UUID termId);

    @Query("SELECT te.term.id, COUNT(te.id) FROM TermEnrollment te GROUP BY te.term.id")
    List<Object[]> countGroupedByTermId();

    @Query("SELECT te.user.id FROM TermEnrollment te WHERE te.term.id = :termId")
    List<UUID> findUserIdsByTermId(@Param("termId") UUID termId);

    @Query(value = """
            SELECT DISTINCT u.id
            FROM term_enrollment te
            JOIN user_account u ON u.id = te.user_id
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE te.term_id = :termId
              AND u.is_active = true
              AND LOWER(r.name) = 'student'
            """, nativeQuery = true)
    List<UUID> findActiveStudentIdsByTermId(@Param("termId") UUID termId);

    @Query(value = """
            SELECT DISTINCT u.id
            FROM term_enrollment te
            JOIN user_account u ON u.id = te.user_id
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE te.term_id = :termId
              AND u.is_active = true
              AND LOWER(r.name) = 'student'
              AND u.email IS NOT NULL
              AND BTRIM(u.email) <> ''
              AND NOT EXISTS (
                  SELECT 1
                  FROM lab_submission s
                  WHERE s.user_id = u.id AND s.lab_id = :labId
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM lab_deadline_email_sent e
                  WHERE e.user_id = u.id
                    AND e.lab_id = :labId
                    AND e.threshold_hours = :thresholdHours
              )
            """, nativeQuery = true)
    List<UUID> findActiveStudentIdsForDeadlineEmail(
            @Param("termId") UUID termId,
            @Param("labId") UUID labId,
            @Param("thresholdHours") short thresholdHours);

    boolean existsByUser_IdAndTerm_Id(UUID userId, UUID termId);

    boolean existsByUser_IdAndTerm_CurrentTrue(UUID userId);

    Optional<TermEnrollment> findByUser_IdAndTerm_Id(UUID userId, UUID termId);

    @Query("""
            SELECT te FROM TermEnrollment te
            JOIN FETCH te.user u
            LEFT JOIN FETCH u.roles
            WHERE te.term.id = :termId
            ORDER BY u.fullName
            """)
    List<TermEnrollment> findByTermIdWithUser(@Param("termId") UUID termId);

    void deleteByUser_Id(UUID userId);

    void deleteByTerm_Id(UUID termId);

    /**
     * Active student-only accounts whose earliest enrolled quarter ordinal is at or before the threshold.
     * Ordinal: (start_year - 2000) * 4 + (term_number - 1) from academic_years.year_label.
     */
    @Query(value = """
            SELECT te.user_id
            FROM term_enrollment te
            JOIN term t ON t.id = te.term_id
            JOIN academic_years ay ON ay.id = t.academic_year_id
            JOIN user_account u ON u.id = te.user_id
            JOIN user_role ur ON ur.user_id = u.id
            JOIN role r ON r.id = ur.role_id
            WHERE u.is_active = true
              AND LOWER(r.name) = 'student'
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_role ur2
                  JOIN role r2 ON r2.id = ur2.role_id
                  WHERE ur2.user_id = u.id
                    AND LOWER(r2.name) IN ('lecturer', 'teacher')
              )
            GROUP BY te.user_id
            HAVING MIN(
                (CAST(SPLIT_PART(ay.year_label, '-', 1) AS integer) - 2000) * 4
                + (t.term_number - 1)
            ) <= :maxFirstOrdinal
            """, nativeQuery = true)
    List<UUID> findStudentIdsWithFirstEnrollmentOrdinalAtMost(@Param("maxFirstOrdinal") int maxFirstOrdinal);
}
