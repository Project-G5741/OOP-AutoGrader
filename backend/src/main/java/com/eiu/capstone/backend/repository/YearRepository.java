package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YearRepository extends JpaRepository<AcademicYear, Integer> {
}