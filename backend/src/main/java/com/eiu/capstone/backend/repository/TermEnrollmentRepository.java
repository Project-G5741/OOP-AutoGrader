package com.eiu.capstone.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.TermEnrollment;

public interface TermEnrollmentRepository extends JpaRepository<TermEnrollment, UUID> {
    long countByTerm_Id(UUID termId);
}
