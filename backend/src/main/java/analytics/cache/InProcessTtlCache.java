package com.eiu.capstone.backend.analytics.cache;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class InProcessTtlCache<K, V> {

    private final long ttlSeconds;
    private final ConcurrentHashMap<K, CachedEntry<V>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Object> loadLocks = new ConcurrentHashMap<>();

    InProcessTtlCache(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    V get(K key, Supplier<V> loader) {
        CachedEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }
        if (entry != null) {
            cache.remove(key, entry);
        }

        Object lock = loadLocks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            try {
                entry = cache.get(key);
                if (entry != null && !entry.isExpired()) {
                    return entry.value();
                }
                V value = loader.get();
                cache.put(key, new CachedEntry<>(value, Instant.now().plusSeconds(ttlSeconds)));
                return value;
            } finally {
                loadLocks.remove(key, lock);
            }
        }
    }

    void invalidate(K key) {
        cache.remove(key);
    }

    private record CachedEntry<V>(V value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
