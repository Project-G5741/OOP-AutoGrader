package com.eiu.capstone.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eiu.capstone.backend.model.TestcaseInvocation;

public interface TestcaseInvocationRepository extends JpaRepository<TestcaseInvocation, UUID> {

    @Query("""
            SELECT i FROM TestcaseInvocation i
            LEFT JOIN FETCH i.constructor
            LEFT JOIN FETCH i.method
            LEFT JOIN FETCH i.receiverConstructor
            WHERE i.testcase.id IN :testcaseIds
            """)
    List<TestcaseInvocation> findByTestcase_IdIn(@Param("testcaseIds") Collection<UUID> testcaseIds);
}
