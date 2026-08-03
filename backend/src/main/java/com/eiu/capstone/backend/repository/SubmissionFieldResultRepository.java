package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.SubmissionFieldResult;

public interface SubmissionFieldResultRepository extends JpaRepository<SubmissionFieldResult, UUID> {

    List<SubmissionFieldResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionFieldResult> findBySubmissionAndField(LabSubmission submission, Field field);

    void deleteBySubmission(LabSubmission submission);

    List<SubmissionFieldResult> findBySubmission_Id(UUID submissionId);
}