package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_notification_dismiss")
public class StudentNotificationDismiss {

    @EmbeddedId
    private StudentNotificationDismissKey id;

    @Column(name = "dismissed_at", nullable = false)
    private OffsetDateTime dismissedAt;

    public StudentNotificationDismissKey getId() {
        return id;
    }

    public void setId(StudentNotificationDismissKey id) {
        this.id = id;
    }

    public OffsetDateTime getDismissedAt() {
        return dismissedAt;
    }

    public void setDismissedAt(OffsetDateTime dismissedAt) {
        this.dismissedAt = dismissedAt;
    }
}
