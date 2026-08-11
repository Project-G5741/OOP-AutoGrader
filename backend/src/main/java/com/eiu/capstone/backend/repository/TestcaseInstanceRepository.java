package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.TestcaseInstance;

public interface TestcaseInstanceRepository extends JpaRepository<TestcaseInstance, UUID> {

    List<TestcaseInstance> findByTestcase_IdIn(Collection<UUID> testcaseIds);
}
