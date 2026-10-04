package com.eiu.capstone.backend.analytics.cache;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;

@Component
public class AnalyticsDashboardCache {

    private final InProcessTtlCache<CacheKey, AnalyticsDashboardResponse> cache;

    public AnalyticsDashboardCache(
            @Value("${app.analytics.dashboard-cache-ttl-seconds:180}") long ttlSeconds) {
        this.cache = new InProcessTtlCache<>(ttlSeconds);
    }

    public AnalyticsDashboardResponse get(CacheKey key, Loader loader) {
        return cache.get(key, loader::load);
    }

    public record CacheKey(UUID academicYearId, UUID semesterId, UUID labId, String course) {
        public CacheKey {
            course = course == null ? "" : course.trim().toLowerCase();
        }
    }

    @FunctionalInterface
    public interface Loader {
        AnalyticsDashboardResponse load();
    }
}
