package com.eiu.capstone.backend.analytics.cache;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;

@Component
public class LabStatisticsCache {

    private final InProcessTtlCache<UUID, LabStatisticsResponse> cache;

    public LabStatisticsCache(
            @Value("${app.analytics.lab-statistics-cache-ttl-seconds:120}") long ttlSeconds,
            @Value("${app.analytics.lab-statistics-cache-max-size:512}") int maxSize) {
        this.cache = new InProcessTtlCache<>(ttlSeconds, maxSize);
    }

    public LabStatisticsResponse get(UUID labId, Loader loader) {
        return cache.get(labId, loader::load);
    }

    public void invalidate(UUID labId) {
        cache.invalidate(labId);
    }

    public void sweepExpired() {
        cache.sweepExpired();
    }

    @FunctionalInterface
    public interface Loader {
        LabStatisticsResponse load();
    }
}
