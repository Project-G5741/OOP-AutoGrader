package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.SolutionFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolutionFileRepository extends JpaRepository<SolutionFile, Integer> {
}