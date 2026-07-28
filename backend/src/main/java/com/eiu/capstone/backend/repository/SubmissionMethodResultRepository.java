package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.SubmissionMethodResult;

public interface SubmissionMethodResultRepository extends JpaRepository<SubmissionMethodResult, UUID> {

    List<SubmissionMethodResult> findBySubmission(LabSubmission submission);

    Optional<SubmissionMethodResult> findBySubmissionAndMethod(LabSubmission submission, Method method);
}