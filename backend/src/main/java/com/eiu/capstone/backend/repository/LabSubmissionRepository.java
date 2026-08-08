package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.UserAccount;

public interface LabSubmissionRepository extends JpaRepository<LabSubmission, UUID> {

    /**
     * Maps to the lab_submission_user_lab_attempt_key unique constraint
     * (user_id, lab_id, attempt_number). SubmissionController uses this to decide
     * whether to update an existing attempt's row or insert a new one.
     */
    Optional<LabSubmission> findByUserAndLabAndAttemptNumber(UserAccount user, Lab lab, Integer attemptNumber);

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
}