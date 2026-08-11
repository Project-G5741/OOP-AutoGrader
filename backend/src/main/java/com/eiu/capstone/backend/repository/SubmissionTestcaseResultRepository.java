package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.SubmissionTestcaseResult;

public interface SubmissionTestcaseResultRepository extends JpaRepository<SubmissionTestcaseResult, UUID> {

    @Query("""
            SELECT DISTINCT r FROM SubmissionTestcaseResult r
            JOIN FETCH r.testcase
            LEFT JOIN FETCH r.assertionResults
            WHERE r.submission.id = :submissionId
            """)
    List<SubmissionTestcaseResult> findBySubmission_IdWithTestcase(@Param("submissionId") UUID submissionId);
}
