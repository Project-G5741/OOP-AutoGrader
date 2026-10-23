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
        name = "submission_field_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_field_result_key",
                columnNames = {"submission_id", "field_id"}
        )
)
public class SubmissionFieldResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "sfr_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false,
            foreignKey = @ForeignKey(name = "sfr_field_id_fkey"))
    private Field field;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    public SubmissionFieldResult() {}

    public UUID getId() { return id; }

    public LabSubmission getSubmission() { return submission; }
    public void setSubmission(LabSubmission submission) { this.submission = submission; }

    public Field getField() { return field; }
    public void setField(Field field) { this.field = field; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}