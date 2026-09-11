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
}
