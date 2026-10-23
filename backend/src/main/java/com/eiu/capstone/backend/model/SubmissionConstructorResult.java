package com.eiu.capstone.backend.model;

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
        name = "submission_constructor_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_constructor_result_key",
                columnNames = {"submission_id", "constructor_id"}
        )
)
public class SubmissionConstructorResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "scnr_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "constructor_id", nullable = false,
            foreignKey = @ForeignKey(name = "scnr_constructor_id_fkey"))
    private Constructor constructor;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    public SubmissionConstructorResult() {}

    public UUID getId() { return id; }

    public LabSubmission getSubmission() { return submission; }
    public void setSubmission(LabSubmission submission) { this.submission = submission; }

    public Constructor getConstructor() { return constructor; }
    public void setConstructor(Constructor constructor) { this.constructor = constructor; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}