package com.eiu.capstone.backend.grading;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.model.SubmissionChallengeResult;

public interface GradingResultJdbcWriter {

    record UploadWriteResult(int attemptNumber, OffsetDateTime lastSubmittedAt) {}

    UploadWriteResult persistUpload(UUID submissionId,
                                    UUID userId,
                                    UUID labId,
                                    BigDecimal score,
                                    OffsetDateTime submittedAt,
                                    List<SubmissionChallengeResult> challengeRows,
                                    String desktopPackVersion);

    void upsertChallengeResults(List<SubmissionChallengeResult> rows);

    void upsertDetails(GradingDetailPersistPayload payload);
}
