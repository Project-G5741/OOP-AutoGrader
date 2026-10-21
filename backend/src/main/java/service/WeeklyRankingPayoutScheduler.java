package com.eiu.capstone.backend.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!desktop")
public class WeeklyRankingPayoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyRankingPayoutScheduler.class);

    private final WeeklyRankingPayoutService weeklyRankingPayoutService;

    public WeeklyRankingPayoutScheduler(WeeklyRankingPayoutService weeklyRankingPayoutService) {
        this.weeklyRankingPayoutService = weeklyRankingPayoutService;
    }

    @Scheduled(cron = "59 59 23 * * 0", zone = "Asia/Ho_Chi_Minh")
    public void paySundayFreeze() {
        runCatchUp();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        runCatchUp();
    }

    private void runCatchUp() {
        try {
            weeklyRankingPayoutService.catchUpUnpaidWeeks(Instant.now());
        } catch (Exception ex) {
            log.error("Weekly ranking payout failed", ex);
        }
    }
}
