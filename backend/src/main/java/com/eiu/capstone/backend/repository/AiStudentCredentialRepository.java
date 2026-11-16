package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.ai.AiStudentCredential;

public interface AiStudentCredentialRepository extends JpaRepository<AiStudentCredential, UUID> {

    Optional<AiStudentCredential> findByStudentId(UUID studentId);

    void deleteByStudentId(UUID studentId);
}
