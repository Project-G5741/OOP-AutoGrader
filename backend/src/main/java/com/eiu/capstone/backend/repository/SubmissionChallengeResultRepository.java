package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionChallengeResult;

public interface SubmissionChallengeResultRepository extends JpaRepository<SubmissionChallengeResult, UUID> {

    List<SubmissionChallengeResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionChallengeResult> findBySubmissionAndChallenge(LabSubmission submission, Challenge challenge);

    void deleteBySubmission(LabSubmission submission);
}