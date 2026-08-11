package com.eiu.capstone.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.SubmissionTestcaseAssertionResult;

public interface SubmissionTestcaseAssertionResultRepository
        extends JpaRepository<SubmissionTestcaseAssertionResult, UUID> {}
