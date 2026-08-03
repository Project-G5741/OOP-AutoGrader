package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionConstructorResult;

public interface SubmissionConstructorResultRepository extends JpaRepository<SubmissionConstructorResult, UUID> {

    List<SubmissionConstructorResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionConstructorResult> findBySubmissionAndConstructor(LabSubmission submission, Constructor constructor);

    void deleteBySubmission(LabSubmission submission);

    List<SubmissionConstructorResult> findBySubmission_Id(UUID submissionId);

}