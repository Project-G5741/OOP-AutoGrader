package com.eiu.capstone.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_mascot_practice_revoked_jti")
public class MascotPracticeRevokedJti {

    @Id
    @Column(name = "jti", nullable = false, length = 64)
    private String jti;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    public MascotPracticeRevokedJti() {
    }

    public MascotPracticeRevokedJti(String jti, Instant revokedAt) {
        this.jti = jti;
        this.revokedAt = revokedAt;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
