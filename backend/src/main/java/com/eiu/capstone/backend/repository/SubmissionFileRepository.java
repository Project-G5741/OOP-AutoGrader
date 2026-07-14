package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.SubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, Integer> {
}