package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestResultRepository extends JpaRepository<TestResult, Integer> {
}