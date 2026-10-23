package com.eiu.capstone.backend.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StudentMascotPurchaseKey implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mascot_id", nullable = false, length = 32)
    private String mascotId;

    public StudentMascotPurchaseKey() {
    }

    public StudentMascotPurchaseKey(UUID userId, String mascotId) {
        this.userId = userId;
        this.mascotId = mascotId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getMascotId() {
        return mascotId;
    }

    public void setMascotId(String mascotId) {
        this.mascotId = mascotId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudentMascotPurchaseKey key)) {
            return false;
        }
        return Objects.equals(userId, key.userId) && Objects.equals(mascotId, key.mascotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, mascotId);
    }
}
