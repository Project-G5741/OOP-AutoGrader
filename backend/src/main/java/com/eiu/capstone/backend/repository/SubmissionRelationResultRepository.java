package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.SubmissionRelationResult;

public interface SubmissionRelationResultRepository extends JpaRepository<SubmissionRelationResult, UUID> {

    @Query("SELECT r FROM SubmissionRelationResult r "
            + "JOIN FETCH r.classRelation cr "
            + "JOIN FETCH cr.classEntity "
            + "JOIN FETCH cr.targetClassEntity "
            + "JOIN FETCH cr.relationType "
            + "WHERE r.submission.id = :submissionId")
    List<SubmissionRelationResult> findBySubmission_IdWithRelation(@Param("submissionId") UUID submissionId);
}
