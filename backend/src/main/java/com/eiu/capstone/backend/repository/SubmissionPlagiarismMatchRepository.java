package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.SubmissionPlagiarismMatch;

public interface SubmissionPlagiarismMatchRepository extends JpaRepository<SubmissionPlagiarismMatch, UUID> {

    List<SubmissionPlagiarismMatch> findByLabIdAndFlaggedTrue(UUID labId);

    List<SubmissionPlagiarismMatch> findByLabId(UUID labId);

    List<SubmissionPlagiarismMatch> findByFlaggedTrue();

    @Query("SELECT DISTINCT m.labId FROM SubmissionPlagiarismMatch m")
    List<UUID> findDistinctLabIds();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from SubmissionPlagiarismMatch m
            where m.submissionId = :submissionId or m.otherSubmissionId = :submissionId
            """)
    void deleteInvolvingSubmission(@Param("submissionId") UUID submissionId);
}
