package com.eiu.capstone.backend.analytics.cache;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.service.MasterDataCache;
import com.eiu.capstone.backend.service.StudentTermAccessService;

/**
 * Proactively removes TTL-expired entries from bounded in-process caches so
 * dead keys do not wait for a same-key re-hit.
 */
@Component
public class InProcessTtlCacheSweeper {

    private final AnalyticsDashboardCache analyticsDashboardCache;
    private final LabStatisticsCache labStatisticsCache;
    private final MasterDataCache masterDataCache;
    private final StudentTermAccessService studentTermAccessService;

    public InProcessTtlCacheSweeper(AnalyticsDashboardCache analyticsDashboardCache,
                                    LabStatisticsCache labStatisticsCache,
                                    MasterDataCache masterDataCache,
                                    StudentTermAccessService studentTermAccessService) {
        this.analyticsDashboardCache = analyticsDashboardCache;
        this.labStatisticsCache = labStatisticsCache;
        this.masterDataCache = masterDataCache;
        this.studentTermAccessService = studentTermAccessService;
    }

    @Scheduled(fixedDelayString = "${app.cache.sweep-interval-ms:30000}")
    public void sweepExpired() {
        analyticsDashboardCache.sweepExpired();
        labStatisticsCache.sweepExpired();
        masterDataCache.sweepExpired();
        studentTermAccessService.sweepExpired();
    }
}
