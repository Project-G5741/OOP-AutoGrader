package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Constructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConstructorRepository extends JpaRepository<Constructor, UUID> {
}
