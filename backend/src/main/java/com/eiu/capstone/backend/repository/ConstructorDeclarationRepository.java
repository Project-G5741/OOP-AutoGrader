package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.ConstructorDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConstructorDeclarationRepository extends JpaRepository<ConstructorDeclaration, UUID> {
}
