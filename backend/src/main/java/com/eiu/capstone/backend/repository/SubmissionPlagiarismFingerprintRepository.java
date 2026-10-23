package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.SubmissionPlagiarismFingerprint;

public interface SubmissionPlagiarismFingerprintRepository
        extends JpaRepository<SubmissionPlagiarismFingerprint, UUID> {

    List<SubmissionPlagiarismFingerprint> findByLabIdAndUserIdNot(UUID labId, UUID userId);

    List<SubmissionPlagiarismFingerprint> findByLabIdAndUserId(UUID labId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SubmissionPlagiarismFingerprint f WHERE f.submissionId = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") UUID submissionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM SubmissionPlagiarismFingerprint f WHERE f.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
