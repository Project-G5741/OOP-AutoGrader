package com.eiu.capstone.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "submission_plagiarism_match", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"submission_id", "other_submission_id"})
})
public class SubmissionPlagiarismMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lab_id", nullable = false)
    private UUID labId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "other_submission_id", nullable = false)
    private UUID otherSubmissionId;

    @Column(name = "git_match", nullable = false)
    private boolean gitMatch;

    @Column(name = "metadata_match", nullable = false)
    private boolean metadataMatch;

    @Column(name = "hash_similarity", nullable = false, precision = 5, scale = 2)
    private BigDecimal hashSimilarity = BigDecimal.ZERO;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    public SubmissionPlagiarismMatch() {}

    public UUID getId() { return id; }

    public UUID getLabId() { return labId; }
    public void setLabId(UUID labId) { this.labId = labId; }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public UUID getOtherSubmissionId() { return otherSubmissionId; }
    public void setOtherSubmissionId(UUID otherSubmissionId) { this.otherSubmissionId = otherSubmissionId; }

    public boolean isGitMatch() { return gitMatch; }
    public void setGitMatch(boolean gitMatch) { this.gitMatch = gitMatch; }

    public boolean isMetadataMatch() { return metadataMatch; }
    public void setMetadataMatch(boolean metadataMatch) { this.metadataMatch = metadataMatch; }

    public BigDecimal getHashSimilarity() { return hashSimilarity; }
    public void setHashSimilarity(BigDecimal hashSimilarity) { this.hashSimilarity = hashSimilarity; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}
