package com.eiu.capstone.backend.grading;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GradingResultStore {

    private final GradingResultJdbcWriter jdbcWriter;
    private final SubmissionDetailPersistGate persistGate;
    private final ExecutorService persistExecutor;

    public GradingResultStore(GradingResultJdbcWriter jdbcWriter,
                       SubmissionDetailPersistGate persistGate,
                       @Qualifier("persistExecutor") ExecutorService persistExecutor) {
        this.jdbcWriter = jdbcWriter;
        this.persistGate = persistGate;
        this.persistExecutor = persistExecutor;
    }

    public void saveChallengeScores(GradingService.GradingComputationResult computed) {
        jdbcWriter.upsertChallengeResults(computed.challengeResults);
    }

    public void scheduleDetailPersist(UUID submissionId, GradingService.GradingComputationResult computed) {
        GradingDetailPersistPayload payload = GradingDetailPersistPayload.from(computed);
        persistGate.runAsync(submissionId, () -> jdbcWriter.upsertDetails(payload), persistExecutor);
    }

    /** Synchronous scores + details. Used by tests. */
    public void save(GradingService.GradingComputationResult computed) {
        saveChallengeScores(computed);
        jdbcWriter.upsertDetails(GradingDetailPersistPayload.from(computed));
    }
}
