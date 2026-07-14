package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
}