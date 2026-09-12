package com.eiu.capstone.backend.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.UserAccount;

public interface LabSubmissionRepository extends JpaRepository<LabSubmission, UUID> {

    /**
     * Maps to the lab_submission_user_lab_attempt_key unique constraint
     * (user_id, lab_id, attempt_number). Upload always inserts MAX+1; this finder
     * is not on the hot path.
     */
    Optional<LabSubmission> findByUserAndLabAndAttemptNumber(UserAccount user, Lab lab, Integer attemptNumber);

    @Query("""
            SELECT MAX(s.attemptNumber) FROM LabSubmission s
            WHERE s.user.id = :userId AND s.lab.id = :labId
            """)
    Integer findMaxAttemptNumber(@Param("userId") UUID userId, @Param("labId") UUID labId);

    @Query("SELECT s FROM LabSubmission s JOIN FETCH s.user WHERE s.id IN :ids")
    List<LabSubmission> findAllWithUserByIdIn(@Param("ids") java.util.Collection<UUID> ids);

    @Query("""
            SELECT s FROM LabSubmission s
            WHERE s.lab.id = :labId
              AND s.user.id IN :userIds
            """)
    List<LabSubmission> findByLabIdAndUserIdIn(
            @Param("labId") UUID labId,
            @Param("userIds") java.util.Collection<UUID> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM LabSubmission s WHERE s.user.id = :userId")
    void deleteByUser_Id(@Param("userId") UUID userId);

    List<LabSubmission> findByUser_Id(UUID userId);

    /** All of a student's submissions across every lab, most recent first — handy for a history/dashboard view. */
    List<LabSubmission> findByUserOrderBySubmittedAtDesc(UserAccount user);

    /** All attempts a student has made on one specific lab, most recent attempt first. */
    List<LabSubmission> findByUserAndLabOrderByAttemptNumberDesc(UserAccount user, Lab lab);

    /** Latest attempt for student-facing grading display. */
    java.util.Optional<LabSubmission> findFirstByUser_IdAndLab_IdOrderByAttemptNumberDesc(UUID userId, UUID labId);

    long countByUser_IdAndLab_Id(UUID userId, UUID labId);

    @Query("SELECT s FROM LabSubmission s JOIN FETCH s.lab WHERE s.user.id = :userId ORDER BY s.submittedAt DESC")
    List<LabSubmission> findByUser_IdWithLabOrderBySubmittedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT s FROM LabSubmission s JOIN FETCH s.lab WHERE s.user.id = :userId AND s.lab.id = :labId ORDER BY s.attemptNumber DESC")
    List<LabSubmission> findByUser_IdAndLab_IdWithLabOrderByAttemptNumberDesc(
            @Param("userId") UUID userId,
            @Param("labId") UUID labId);

    @Query(
            value = """
                    SELECT s FROM LabSubmission s
                    JOIN FETCH s.lab l
                    JOIN FETCH l.term t
                    JOIN FETCH t.academicYear
                    WHERE s.user.id = :userId
                    """,
            countQuery = "SELECT COUNT(s) FROM LabSubmission s WHERE s.user.id = :userId")
    Page<LabSubmission> findHistoryPageByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = """
                    SELECT s FROM LabSubmission s
                    JOIN FETCH s.lab l
                    JOIN FETCH l.term t
                    JOIN FETCH t.academicYear
                    WHERE s.user.id = :userId AND s.lab.id = :labId
                    """,
            countQuery = "SELECT COUNT(s) FROM LabSubmission s WHERE s.user.id = :userId AND s.lab.id = :labId")
    Page<LabSubmission> findHistoryPageByUserIdAndLabId(
            @Param("userId") UUID userId,
            @Param("labId") UUID labId,
            Pageable pageable);

    long countByUser_Id(UUID userId);

    @Query("""
            SELECT AVG(s.score) FROM LabSubmission s
            WHERE s.user.id = :userId
              AND s.score IS NOT NULL
            """)
    BigDecimal averageScoreForUser(@Param("userId") UUID userId);

    @Query("""
            SELECT AVG(s.score) FROM LabSubmission s
            WHERE s.user.id = :userId
              AND s.lab.id = :labId
              AND s.score IS NOT NULL
            """)
    BigDecimal averageScoreForUserAndLab(@Param("userId") UUID userId, @Param("labId") UUID labId);

    @Query("""
            SELECT MAX(s.score) FROM LabSubmission s
            WHERE s.user.id = :userId
              AND s.score IS NOT NULL
            """)
    BigDecimal bestScoreForUser(@Param("userId") UUID userId);

    @Query("""
            SELECT MAX(s.score) FROM LabSubmission s
            WHERE s.user.id = :userId
              AND s.lab.id = :labId
              AND s.score IS NOT NULL
            """)
    BigDecimal bestScoreForUserAndLab(@Param("userId") UUID userId, @Param("labId") UUID labId);

    /** Best score on this lab before/excluding the current attempt (plagiarism score gate). */
    @Query("""
            SELECT MAX(s.score) FROM LabSubmission s
            WHERE s.user.id = :userId
              AND s.lab.id = :labId
              AND s.id <> :excludeSubmissionId
              AND s.score IS NOT NULL
            """)
    BigDecimal bestScoreForUserAndLabExcludingSubmission(
            @Param("userId") UUID userId,
            @Param("labId") UUID labId,
            @Param("excludeSubmissionId") UUID excludeSubmissionId);

    @Query("SELECT COUNT(DISTINCT s.lab.id) FROM LabSubmission s WHERE s.user.id = :userId")
    long countDistinctLabsByUserId(@Param("userId") UUID userId);

    /** Earliest submission time per user in a lab — used for plagiarism role (victim vs plagiarizer). */
    @Query("""
            SELECT s.user.id, MIN(s.submittedAt)
            FROM LabSubmission s
            WHERE s.lab.id = :labId
              AND s.user.id IN :userIds
            GROUP BY s.user.id
            """)
    List<Object[]> findEarliestSubmittedAtByLabAndUserIds(
            @Param("labId") UUID labId,
            @Param("userIds") java.util.Collection<UUID> userIds);
}