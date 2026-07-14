package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabRepository extends JpaRepository<Lab, Integer> {
}