package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.MethodDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MethodDeclarationRepository extends JpaRepository<MethodDeclaration, UUID> {
}
