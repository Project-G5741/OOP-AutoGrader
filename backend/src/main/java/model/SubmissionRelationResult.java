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
        name = "submission_relation_result",
        uniqueConstraints = @UniqueConstraint(
                name = "submission_relation_result_key",
                columnNames = {"submission_id", "class_relation_id"}
        )
)
public class SubmissionRelationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false,
            foreignKey = @ForeignKey(name = "srr_submission_id_fkey"))
    private LabSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_relation_id", nullable = false,
            foreignKey = @ForeignKey(name = "srr_class_relation_id_fkey"))
    private ClassRelation classRelation;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect = false;

    public SubmissionRelationResult() {}

    public UUID getId() { return id; }

    public LabSubmission getSubmission() { return submission; }
    public void setSubmission(LabSubmission submission) { this.submission = submission; }

    public ClassRelation getClassRelation() { return classRelation; }
    public void setClassRelation(ClassRelation classRelation) { this.classRelation = classRelation; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}
