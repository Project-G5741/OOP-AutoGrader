package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Integer> {
}