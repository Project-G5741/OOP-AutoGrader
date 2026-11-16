package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.ai.AiFeedbackCache;

public interface AiFeedbackCacheRepository extends JpaRepository<AiFeedbackCache, UUID> {

    Optional<AiFeedbackCache> findByStudentIdAndSubmissionIdAndLearningAreaAndLanguageAndPolicyVersion(
            UUID studentId,
            UUID submissionId,
            String learningArea,
            String language,
            String policyVersion);
}
