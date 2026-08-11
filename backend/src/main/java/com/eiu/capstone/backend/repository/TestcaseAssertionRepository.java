package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.TestcaseAssertion;

public interface TestcaseAssertionRepository extends JpaRepository<TestcaseAssertion, UUID> {

    List<TestcaseAssertion> findByTestcase_IdInOrderByOrderIndexAsc(Collection<UUID> testcaseIds);
}
