package com.eiu.capstone.backend.model;

import java.math.BigDecimal;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.eiu.capstone.backend.utility.TimeUtil;

@Entity
@Table(
        name = "lab_submission",
        uniqueConstraints = @UniqueConstraint(
                name = "lab_submission_user_lab_attempt_key",
                columnNames = {"user_id", "lab_id", "attempt_number"}
        )
)
public class LabSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "lab_submission_user_id_fkey"))
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "lab_submission_lab_id_fkey"))
    private Lab lab;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    public LabSubmission() {}

    @PrePersist
    protected void onCreate() {
        if (score == null) {
            score = BigDecimal.ZERO;
        }
        if (submittedAt == null) {
            submittedAt = TimeUtil.nowInVietnam();
        }
    }

    public UUID getId() { return id; }

    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }

    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
}