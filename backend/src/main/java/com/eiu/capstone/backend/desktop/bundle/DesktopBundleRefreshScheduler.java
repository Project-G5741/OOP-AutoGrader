package com.eiu.capstone.backend.desktop.bundle;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Optionally re-download bundle zips from stable Release URLs without a container restart
 * (after GitHub asset replace on {@code desktop-practice} / {@code desktop-lecturer-bulk}).
 */
@Component
@Profile("!desktop & !desktop-lecturer")
@ConditionalOnExpression("${app.desktop.bundle.refresh-interval-minutes:0} > 0")
public class DesktopBundleRefreshScheduler {

    private final DesktopBundlePrefetchService prefetchService;

    public DesktopBundleRefreshScheduler(DesktopBundlePrefetchService prefetchService) {
        this.prefetchService = prefetchService;
    }

    @Scheduled(fixedDelayString = "#{${app.desktop.bundle.refresh-interval-minutes} * 60 * 1000}")
    public void scheduledRefresh() {
        prefetchService.refreshAll();
    }
}