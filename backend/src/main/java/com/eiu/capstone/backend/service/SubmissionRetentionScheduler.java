package com.eiu.capstone.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.cache.LecturerOverviewCache;

@Component
@Profile("!desktop")
public class SubmissionRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubmissionRetentionScheduler.class);

    private final SubmissionRetentionService submissionRetentionService;
    private final LecturerOverviewCache lecturerOverviewCache;

    public SubmissionRetentionScheduler(
            SubmissionRetentionService submissionRetentionService,
            LecturerOverviewCache lecturerOverviewCache) {
        this.submissionRetentionService = submissionRetentionService;
        this.lecturerOverviewCache = lecturerOverviewCache;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Ho_Chi_Minh")
    public void pruneSubmissionsNightly() {
        if (!submissionRetentionService.isEnabled()) {
            return;
        }
        try {
            int pairs = submissionRetentionService.enforceAllOverCap();
            if (pairs > 0) {
                lecturerOverviewCache.invalidate();
                log.info("Submission retention nightly job processed {} student(s)", pairs);
            }
        } catch (Exception ex) {
            log.warn("Submission retention nightly job skipped: {}", ex.getMessage());
        }
    }
}
