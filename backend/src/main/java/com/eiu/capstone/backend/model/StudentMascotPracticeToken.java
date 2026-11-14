package com.eiu.capstone.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_mascot_practice_token")
public class StudentMascotPracticeToken {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "jti", nullable = false, length = 64)
    private String jti;

    @Column(name = "compact_token", nullable = false, length = 2048)
    private String compactToken;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getCompactToken() {
        return compactToken;
    }

    public void setCompactToken(String compactToken) {
        this.compactToken = compactToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
