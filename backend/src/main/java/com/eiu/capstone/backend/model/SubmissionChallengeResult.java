package com.eiu.capstone.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "submission_challenge_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_challenge_result_key",
                columnNames = {"submission_id", "challenge_id"}
        )
)
public class SubmissionChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "scr_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(name = "scr_challenge_id_fkey"))
    private Challenge challenge;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    @Column(name = "score", nullable = false, precision = 6, scale = 2)
    private BigDecimal score = BigDecimal.ZERO;

    public SubmissionChallengeResult() {}

    public UUID getId() { return id; }

    public LabSubmission getSubmission() { return submission; }
    public void setSubmission(LabSubmission submission) { this.submission = submission; }

    public Challenge getChallenge() { return challenge; }
    public void setChallenge(Challenge challenge) { this.challenge = challenge; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score == null ? BigDecimal.ZERO : score; }
}