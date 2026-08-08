package com.eiu.capstone.backend.analytics.cache;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;

@Component
public class LabStatisticsCache {

    private final InProcessTtlCache<UUID, LabStatisticsResponse> cache;

    public LabStatisticsCache(
            @Value("${app.analytics.lab-stats-cache-ttl-seconds:120}") long ttlSeconds) {
        this.cache = new InProcessTtlCache<>(ttlSeconds);
    }

    public LabStatisticsResponse get(UUID labId, Loader loader) {
        return cache.get(labId, loader::load);
    }

    public void invalidate(UUID labId) {
        cache.invalidate(labId);
    }

    @FunctionalInterface
    public interface Loader {
        LabStatisticsResponse load();
    }
}
