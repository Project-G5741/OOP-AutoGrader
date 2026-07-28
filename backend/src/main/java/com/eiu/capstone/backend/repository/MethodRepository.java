package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Method;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MethodRepository extends JpaRepository<Method, UUID> {
}
