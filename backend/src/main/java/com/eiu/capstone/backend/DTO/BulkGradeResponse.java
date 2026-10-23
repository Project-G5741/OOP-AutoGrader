package com.eiu.capstone.backend.DTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ephemeral one-student bulk grade payload. Not persisted to roster/submission tables.
 */
public class BulkGradeResponse {

    private final String irn;
    private final String studentFolder;
    private final BigDecimal score;
    private final Map<UUID, Integer> challengeResult;
    private final Map<String, ChallengeDetailBundleDTO> labResult;
    private final List<String> fileHashes;

    public BulkGradeResponse(String irn,
                             String studentFolder,
                             BigDecimal score,
                             Map<UUID, Integer> challengeResult,
                             Map<String, ChallengeDetailBundleDTO> labResult,
                             List<String> fileHashes) {
        this.irn = irn;
        this.studentFolder = studentFolder;
        this.score = score;
        this.challengeResult = challengeResult;
        this.labResult = labResult;
        this.fileHashes = fileHashes;
    }

    public String getIrn() {
        return irn;
    }

    public String getStudentFolder() {
        return studentFolder;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Map<UUID, Integer> getChallengeResult() {
        return challengeResult;
    }

    @JsonProperty("lab_result")
    public Map<String, ChallengeDetailBundleDTO> getLabResult() {
        return labResult;
    }

    public List<String> getFileHashes() {
        return fileHashes;
    }
}
