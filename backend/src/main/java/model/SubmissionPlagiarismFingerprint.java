package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.eiu.capstone.backend.utility.TimeUtil;

@Entity
@Table(name = "submission_plagiarism_fingerprint")
public class SubmissionPlagiarismFingerprint {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "lab_id", nullable = false)
    private UUID labId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "git_commit_hashes", columnDefinition = "text")
    private String gitCommitHashes;

    @Column(name = "metadata_canonical", columnDefinition = "text")
    private String metadataCanonical;

    @Column(name = "file_hashes", columnDefinition = "text")
    private String fileHashes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public SubmissionPlagiarismFingerprint() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = TimeUtil.nowInVietnam();
        }
    }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public UUID getLabId() { return labId; }
    public void setLabId(UUID labId) { this.labId = labId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getGitCommitHashes() { return gitCommitHashes; }
    public void setGitCommitHashes(String gitCommitHashes) { this.gitCommitHashes = gitCommitHashes; }

    public String getMetadataCanonical() { return metadataCanonical; }
    public void setMetadataCanonical(String metadataCanonical) { this.metadataCanonical = metadataCanonical; }

    public String getFileHashes() { return fileHashes; }
    public void setFileHashes(String fileHashes) { this.fileHashes = fileHashes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
