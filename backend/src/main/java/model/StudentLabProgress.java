package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_lab_progress")
public class StudentLabProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "student_lab_progress_user_id_fkey"))
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false,
            foreignKey = @ForeignKey(name = "student_lab_progress_lab_id_fkey"))
    private Lab lab;

    @Column(name = "highest_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal highestScore = BigDecimal.ZERO;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "best_submission_id")
    private UUID bestSubmissionId;

    @Column(name = "first_submitted_at")
    private OffsetDateTime firstSubmittedAt;

    @Column(name = "last_submitted_at")
    private OffsetDateTime lastSubmittedAt;

    public StudentLabProgress() {}

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public Lab getLab() {
        return lab;
    }

    public void setLab(Lab lab) {
        this.lab = lab;
    }

    public BigDecimal getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(BigDecimal highestScore) {
        this.highestScore = highestScore;
    }

    public Integer getAttemptsCount() {
        return attemptsCount;
    }

    public void setAttemptsCount(Integer attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public UUID getBestSubmissionId() {
        return bestSubmissionId;
    }

    public void setBestSubmissionId(UUID bestSubmissionId) {
        this.bestSubmissionId = bestSubmissionId;
    }

    public OffsetDateTime getFirstSubmittedAt() {
        return firstSubmittedAt;
    }

    public void setFirstSubmittedAt(OffsetDateTime firstSubmittedAt) {
        this.firstSubmittedAt = firstSubmittedAt;
    }

    public OffsetDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(OffsetDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }
}
