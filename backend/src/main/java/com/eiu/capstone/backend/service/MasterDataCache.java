package com.eiu.capstone.backend.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.analytics.cache.InProcessTtlCache;

@Component
public class MasterDataCache {

    private static final String CACHE_KEY = "master-data";

    private final MasterDataResolver masterDataResolver;
    private final InProcessTtlCache<String, Map<Integer, String>> cache;

    public MasterDataCache(MasterDataResolver masterDataResolver,
                           @Value("${app.master-data-cache-ttl-minutes:60}") long ttlMinutes,
                           @Value("${app.master-data-cache-max-size:1}") int maxSize) {
        this.masterDataResolver = masterDataResolver;
        long ttlSeconds = Math.max(ttlMinutes, 0) * 60;
        this.cache = new InProcessTtlCache<>(ttlSeconds, maxSize);
    }

    public Map<Integer, String> get() {
        return cache.get(CACHE_KEY, masterDataResolver::loadAll);
    }

    public void invalidate() {
        cache.invalidate(CACHE_KEY);
    }

    public void sweepExpired() {
        cache.sweepExpired();
    }
}
