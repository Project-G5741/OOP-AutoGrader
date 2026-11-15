package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_notification_read")
public class StudentNotificationRead {

    @EmbeddedId
    private StudentNotificationReadKey id;

    @Column(name = "read_at", nullable = false)
    private OffsetDateTime readAt;

    public StudentNotificationReadKey getId() {
        return id;
    }

    public void setId(StudentNotificationReadKey id) {
        this.id = id;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }
}
