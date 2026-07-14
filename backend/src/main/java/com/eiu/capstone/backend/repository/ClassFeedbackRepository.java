package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.ClassFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassFeedbackRepository extends JpaRepository<ClassFeedback, Integer> {
}