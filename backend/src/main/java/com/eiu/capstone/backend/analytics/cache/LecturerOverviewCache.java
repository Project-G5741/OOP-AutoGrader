package com.eiu.capstone.backend.analytics.cache;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;

@Component
public class LecturerOverviewCache {

    private final long ttlSeconds;
    private volatile CachedEntry cached;

    public LecturerOverviewCache(
            @Value("${app.analytics.overview-cache-ttl-seconds:90}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public LecturerOverviewResponse get(Loader loader) {
        CachedEntry entry = cached;
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        synchronized (this) {
            entry = cached;
            if (entry != null && !entry.isExpired()) {
                return entry.value();
            }
            LecturerOverviewResponse value = loader.load();
            cached = new CachedEntry(value, Instant.now().plusSeconds(ttlSeconds));
            return value;
        }
    }

    public void invalidate() {
        cached = null;
    }

    private record CachedEntry(LecturerOverviewResponse value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @FunctionalInterface
    public interface Loader {
        LecturerOverviewResponse load();
    }
}
