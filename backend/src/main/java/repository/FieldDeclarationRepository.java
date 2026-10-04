package com.eiu.capstone.backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.FieldDeclaration;

public interface FieldDeclarationRepository extends JpaRepository<FieldDeclaration, UUID> {
}