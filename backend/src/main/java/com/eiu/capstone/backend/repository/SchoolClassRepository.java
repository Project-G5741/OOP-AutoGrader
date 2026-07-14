package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Integer> {
}