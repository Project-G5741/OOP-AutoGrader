package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.TestcaseInvocation;

public interface TestcaseInvocationRepository extends JpaRepository<TestcaseInvocation, UUID> {

    List<TestcaseInvocation> findByTestcase_IdIn(Collection<UUID> testcaseIds);
}
