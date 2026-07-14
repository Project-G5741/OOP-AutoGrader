package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Enrollment;
import com.eiu.capstone.backend.model.EnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {
}