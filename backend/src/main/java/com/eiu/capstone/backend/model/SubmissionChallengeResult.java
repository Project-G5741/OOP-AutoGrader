package com.eiu.capstone.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** Null when that pillar does not apply to the challenge. */
    @Column(name = "mmd_score", precision = 6, scale = 2)
    private BigDecimal mmdScore;

    @Column(name = "declaration_score", precision = 6, scale = 2)
    private BigDecimal declarationScore;

    @Column(name = "operational_score", precision = 6, scale = 2)
    private BigDecimal operationalScore;

    /** Versioned MMD member outcomes JSON; null when MMD not applicable or legacy row. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mmd_outcomes")
    private String mmdOutcomes;

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

    public BigDecimal getMmdScore() { return mmdScore; }
    public void setMmdScore(BigDecimal mmdScore) { this.mmdScore = mmdScore; }

    public BigDecimal getDeclarationScore() { return declarationScore; }
    public void setDeclarationScore(BigDecimal declarationScore) { this.declarationScore = declarationScore; }

    public BigDecimal getOperationalScore() { return operationalScore; }
    public void setOperationalScore(BigDecimal operationalScore) { this.operationalScore = operationalScore; }

    public String getMmdOutcomes() { return mmdOutcomes; }
    public void setMmdOutcomes(String mmdOutcomes) { this.mmdOutcomes = mmdOutcomes; }
}