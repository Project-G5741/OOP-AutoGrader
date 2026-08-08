package com.eiu.capstone.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eiu.capstone.backend.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    void deleteByUser_IdAndUsedAtIsNull(UUID userId);

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
}
