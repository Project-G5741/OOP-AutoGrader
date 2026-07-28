package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SubmissionUploadResponse {

    private final UUID submissionId;
    private final String irn;
    private final String requestId;
    private final List<ChallengeUploadResult> challenges;
    private final BigDecimal score;

    public SubmissionUploadResponse(UUID submissionId, String irn, String requestId,
                                     List<ChallengeUploadResult> challenges, BigDecimal score) {
        this.submissionId = submissionId;
        this.irn = irn;
        this.requestId = requestId;
        this.challenges = challenges;
        this.score = score;
    }

    public UUID getSubmissionId() { return submissionId; }
    public String getIrn() { return irn; }
    public String getRequestId() { return requestId; }
    public List<ChallengeUploadResult> getChallenges() { return challenges; }
    public BigDecimal getScore() { return score; }
}