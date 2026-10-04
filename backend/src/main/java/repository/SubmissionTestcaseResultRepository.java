package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.SubmissionTestcaseResult;

public interface SubmissionTestcaseResultRepository extends JpaRepository<SubmissionTestcaseResult, UUID> {

    @Query("""
            SELECT DISTINCT r FROM SubmissionTestcaseResult r
            JOIN FETCH r.testcase
            LEFT JOIN FETCH r.assertionResults ar
            LEFT JOIN FETCH ar.testcaseAssertion
            WHERE r.submission.id = :submissionId
            """)
    List<SubmissionTestcaseResult> findBySubmission_IdWithTestcase(@Param("submissionId") UUID submissionId);

    void deleteBySubmission_Id(UUID submissionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM submission_testcase_assertion_result
            WHERE submission_testcase_result_id IN (
                SELECT str.id
                FROM submission_testcase_result str
                JOIN lab_submission s ON s.id = str.submission_id
                WHERE s.user_id = :userId
            )
            """, nativeQuery = true)
    void deleteAssertionResultsByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM submission_testcase_result
            WHERE submission_id IN (SELECT id FROM lab_submission WHERE user_id = :userId)
            """, nativeQuery = true)
    void deleteByUserId(@Param("userId") UUID userId);
}
