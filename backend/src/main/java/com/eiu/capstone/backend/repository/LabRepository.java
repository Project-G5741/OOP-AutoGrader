package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabRepository extends JpaRepository<Lab, UUID> {
    List<Lab> findByTerm_Id(UUID termId);
}