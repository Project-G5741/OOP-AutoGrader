package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabRepository extends JpaRepository<Lab, UUID> {
    List<Lab> findByTerm_Id(UUID termId);

    @Query("SELECT l FROM Lab l LEFT JOIN FETCH l.term WHERE l.id = :id")
    Optional<Lab> findByIdWithTerm(@Param("id") UUID id);

    @Query("SELECT l FROM Lab l JOIN FETCH l.term WHERE l.deadlineDate IS NOT NULL")
    List<Lab> findAllWithDeadlineAndTerm();
}