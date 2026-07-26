package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.FieldDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FieldDeclarationRepository extends JpaRepository<FieldDeclaration, UUID> {
}
