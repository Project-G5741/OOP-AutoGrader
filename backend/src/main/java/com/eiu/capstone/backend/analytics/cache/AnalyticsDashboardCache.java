package com.eiu.capstone.backend.analytics.cache;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;

@Component
public class AnalyticsDashboardCache {

    private final InProcessTtlCache<CacheKey, AnalyticsDashboardResponse> cache;

    public AnalyticsDashboardCache(
            @Value("${app.analytics.dashboard-cache-ttl-seconds:180}") long ttlSeconds,
            @Value("${app.analytics.dashboard-cache-max-size:256}") int maxSize) {
        this.cache = new InProcessTtlCache<>(ttlSeconds, maxSize);
    }

    public AnalyticsDashboardResponse get(CacheKey key, Loader loader) {
        return cache.get(key, loader::load);
    }

    public void sweepExpired() {
        cache.sweepExpired();
    }

    public int sizeForTests() {
        return cache.size();
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
