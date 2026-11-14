package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.StudentMascotPracticeToken;

public interface StudentMascotPracticeTokenRepository extends JpaRepository<StudentMascotPracticeToken, UUID> {

    Optional<StudentMascotPracticeToken> findByJti(String jti);
}
