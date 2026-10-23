package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;

public interface SubmissionConstructorResultRepository extends JpaRepository<SubmissionConstructorResult, UUID> {

    List<SubmissionConstructorResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionConstructorResult> findBySubmissionAndConstructor(LabSubmission submission, Constructor constructor);

    void deleteBySubmission(LabSubmission submission);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM submission_constructor_result
            WHERE submission_id IN (SELECT id FROM lab_submission WHERE user_id = :userId)
            """, nativeQuery = true)
    void deleteByUserId(@Param("userId") UUID userId);

    List<SubmissionConstructorResult> findBySubmission_Id(UUID submissionId);

    @Query("SELECT r FROM SubmissionConstructorResult r JOIN FETCH r.constructor WHERE r.submission.id = :submissionId")
    List<SubmissionConstructorResult> findBySubmission_IdWithConstructor(@Param("submissionId") UUID submissionId);
}