package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermRepository extends JpaRepository<Term, UUID> {
    List<Term> findByAcademicYear_Id(UUID academicYearId);
    Optional<Term> findByAcademicYear_IdAndTermNumber(UUID academicYearId, int termNumber);
}