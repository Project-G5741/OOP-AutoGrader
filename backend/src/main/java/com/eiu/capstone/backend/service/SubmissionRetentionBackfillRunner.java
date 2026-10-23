package com.eiu.capstone.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;

/**
 * Trims historical submission rows to the retention cap after the API is up.
 * Does not block HTTP startup; runs on a dedicated daemon thread (not {@code persistExecutor}).
 */
@Component
@Profile("!desktop")
public class SubmissionRetentionBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRetentionBackfillRunner.class);

    private final boolean backfillOnStartup;
    private final SubmissionRetentionService submissionRetentionService;
    private final LecturerOverviewCache lecturerOverviewCache;

    public SubmissionRetentionBackfillRunner(
            @Value("${app.submission.retention.backfill-on-startup:true}") boolean backfillOnStartup,
            SubmissionRetentionService submissionRetentionService,
            LecturerOverviewCache lecturerOverviewCache) {
        this.backfillOnStartup = backfillOnStartup;
        this.submissionRetentionService = submissionRetentionService;
        this.lecturerOverviewCache = lecturerOverviewCache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleStartupBackfill() {
        if (!backfillOnStartup || !submissionRetentionService.isEnabled()) {
            return;
        }
        Thread backfill = new Thread(() -> {
            try {
                log.info("Submission retention startup backfill started");
                int pairs = submissionRetentionService.pruneAllExistingOverCap();
                lecturerOverviewCache.invalidate();
                log.info("Submission retention startup backfill finished: {} student(s) processed", pairs);
            } catch (RuntimeException ex) {
                log.warn("Submission retention startup backfill failed: {}", ex.getMessage(), ex);
            }
        }, "submission-retention-backfill");
        backfill.setDaemon(true);
        backfill.start();
    }
}
