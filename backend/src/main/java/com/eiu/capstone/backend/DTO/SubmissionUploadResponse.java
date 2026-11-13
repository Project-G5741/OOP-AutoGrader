package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmissionUploadResponse {

    private final UUID submissionId;
    private final String irn;
    private final String requestId;
    private final Map<UUID, Integer> challengeResult;
    private final BigDecimal score;
    private final Integer attemptNumber;
    private final Integer totalSubmissions;
    private final String latestSubmission;
    private final Map<String, ChallengeDetailBundleDTO> labResult;
    /** False when the attempt was graded and returned without a `lab_submission` row (Admin). */
    private final boolean persisted;

    public SubmissionUploadResponse(UUID submissionId,
                                    String irn,
                                    String requestId,
                                    Map<UUID, Integer> challengeResult,
                                    BigDecimal score,
                                    Integer attemptNumber,
                                    Integer totalSubmissions,
                                    String latestSubmission,
                                    Map<String, ChallengeDetailBundleDTO> labResult) {
        this(submissionId, irn, requestId, challengeResult, score, attemptNumber,
                totalSubmissions, latestSubmission, labResult, true);
    }

    public SubmissionUploadResponse(UUID submissionId,
                                    String irn,
                                    String requestId,
                                    Map<UUID, Integer> challengeResult,
                                    BigDecimal score,
                                    Integer attemptNumber,
                                    Integer totalSubmissions,
                                    String latestSubmission,
                                    Map<String, ChallengeDetailBundleDTO> labResult,
                                    boolean persisted) {
        this.submissionId = submissionId;
        this.irn = irn;
        this.requestId = requestId;
        this.challengeResult = challengeResult;
        this.score = score;
        this.attemptNumber = attemptNumber;
        this.totalSubmissions = totalSubmissions;
        this.latestSubmission = latestSubmission;
        this.labResult = labResult;
        this.persisted = persisted;
    }

    public UUID getSubmissionId() { return submissionId; }
    public String getIrn() { return irn; }
    public String getRequestId() { return requestId; }
    public Map<UUID, Integer> getChallengeResult() { return challengeResult; }
    public BigDecimal getScore() { return score; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public Integer getTotalSubmissions() { return totalSubmissions; }
    public String getLatestSubmission() { return latestSubmission; }

    @JsonProperty("lab_result")
    public Map<String, ChallengeDetailBundleDTO> getLabResult() { return labResult; }

    /** False for Admin dry uploads: detail tabs must come from {@link #getLabResult()}, not a later GET. */
    public boolean isPersisted() { return persisted; }
}
