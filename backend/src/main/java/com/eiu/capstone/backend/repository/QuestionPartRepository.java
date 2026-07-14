package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.QuestionPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionPartRepository extends JpaRepository<QuestionPart, Integer> {
}