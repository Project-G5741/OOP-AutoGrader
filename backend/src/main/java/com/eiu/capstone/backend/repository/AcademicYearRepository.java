package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Optional<AcademicYear> findByYearLabel(String yearLabel);
}