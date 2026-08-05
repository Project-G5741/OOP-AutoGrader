package com.eiu.capstone.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MasterDataCache {

    private static final String CACHE_KEY = "master-data";

    private final MasterDataResolver masterDataResolver;
    private final long ttlMinutes;
    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private final Object loadLock = new Object();

    public MasterDataCache(MasterDataResolver masterDataResolver,
                           @Value("${app.master-data-cache-ttl-minutes:60}") long ttlMinutes) {
        this.masterDataResolver = masterDataResolver;
        this.ttlMinutes = ttlMinutes;
    }

    public Map<Integer, String> get() {
        CachedEntry entry = cache.get(CACHE_KEY);
        if (entry != null && !entry.isExpired()) {
            return entry.data();
        }

        synchronized (loadLock) {
            entry = cache.get(CACHE_KEY);
            if (entry != null && !entry.isExpired()) {
                return entry.data();
            }
            Map<Integer, String> data = masterDataResolver.loadAll();
            cache.put(CACHE_KEY, new CachedEntry(data, Instant.now().plusSeconds(ttlMinutes * 60)));
            return data;
        }
    }

    public void invalidate() {
        cache.remove(CACHE_KEY);
    }

    private record CachedEntry(Map<Integer, String> data, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
