package com.eiu.capstone.backend.ai;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ai_student_credential",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_student_credential_student", columnNames = "student_id"))
public class AiStudentCredential {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** AES-GCM ciphertext (Base64), never plaintext. */
    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "text")
    private String encryptedApiKey;

    @Column(name = "key_hint", nullable = false, length = 16)
    private String keyHint;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed = 0L;

    @Column(name = "token_budget", nullable = false)
    private long tokenBudget = 1_000_000L;

    @Column(name = "notified_50", nullable = false)
    private boolean notified50 = false;

    @Column(name = "notified_80", nullable = false)
    private boolean notified80 = false;

    @Column(name = "notified_90", nullable = false)
    private boolean notified90 = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getEncryptedApiKey() { return encryptedApiKey; }
    public void setEncryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; }
    public String getKeyHint() { return keyHint; }
    public void setKeyHint(String keyHint) { this.keyHint = keyHint; }
    public long getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(long tokensUsed) { this.tokensUsed = tokensUsed; }
    public long getTokenBudget() { return tokenBudget; }
    public void setTokenBudget(long tokenBudget) { this.tokenBudget = tokenBudget; }
    public boolean isNotified50() { return notified50; }
    public void setNotified50(boolean notified50) { this.notified50 = notified50; }
    public boolean isNotified80() { return notified80; }
    public void setNotified80(boolean notified80) { this.notified80 = notified80; }
    public boolean isNotified90() { return notified90; }
    public void setNotified90(boolean notified90) { this.notified90 = notified90; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
