package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.PartGrade;
import com.eiu.capstone.backend.model.PartGradeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartGradeRepository extends JpaRepository<PartGrade, PartGradeId> {
}