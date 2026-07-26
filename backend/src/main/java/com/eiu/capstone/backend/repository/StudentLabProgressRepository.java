package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.StudentLabProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentLabProgressRepository extends JpaRepository<StudentLabProgress, UUID> {
}
