package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.SubmissionPlagiarismFingerprint;

public interface SubmissionPlagiarismFingerprintRepository
        extends JpaRepository<SubmissionPlagiarismFingerprint, UUID> {

    List<SubmissionPlagiarismFingerprint> findByLabIdAndUserIdNot(UUID labId, UUID userId);
}
