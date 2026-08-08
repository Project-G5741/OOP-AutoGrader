package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;

public interface SubmissionChallengeResultRepository extends JpaRepository<SubmissionChallengeResult, UUID> {

    List<SubmissionChallengeResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionChallengeResult> findBySubmissionAndChallenge(LabSubmission submission, Challenge challenge);

    void deleteBySubmission(LabSubmission submission);

    @Query("SELECT r FROM SubmissionChallengeResult r JOIN FETCH r.challenge WHERE r.submission.id = :submissionId")
    List<SubmissionChallengeResult> findBySubmission_IdWithChallenge(@Param("submissionId") UUID submissionId);

    @Query("SELECT r FROM SubmissionChallengeResult r JOIN FETCH r.challenge WHERE r.submission.id IN :submissionIds")
    List<SubmissionChallengeResult> findBySubmission_IdInWithChallenge(@Param("submissionIds") List<UUID> submissionIds);
}