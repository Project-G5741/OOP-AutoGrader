package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "lab_deadline_email_sent", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"lab_id", "user_id", "threshold_hours"})
})
public class LabDeadlineEmailSent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "threshold_hours", nullable = false)
    private short thresholdHours;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    public LabDeadlineEmailSent() {
    }

    public UUID getId() {
        return id;
    }

    public Lab getLab() {
        return lab;
    }

    public void setLab(Lab lab) {
        this.lab = lab;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public short getThresholdHours() {
        return thresholdHours;
    }

    public void setThresholdHours(short thresholdHours) {
        this.thresholdHours = thresholdHours;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
