package com.eiu.capstone.backend.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StudentNotificationReadKey implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notif_key", nullable = false, length = 191)
    private String notifKey;

    public StudentNotificationReadKey() {
    }

    public StudentNotificationReadKey(UUID userId, String notifKey) {
        this.userId = userId;
        this.notifKey = notifKey;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getNotifKey() {
        return notifKey;
    }

    public void setNotifKey(String notifKey) {
        this.notifKey = notifKey;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudentNotificationReadKey key)) {
            return false;
        }
        return Objects.equals(userId, key.userId) && Objects.equals(notifKey, key.notifKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, notifKey);
    }
}
