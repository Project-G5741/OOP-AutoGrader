package com.eiu.capstone.backend.analytics.cache;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Bounded in-process TTL cache: expire-after-write plus a hard max size.
 * Expired entries are removed by {@link #sweepExpired()} (and on hit) without
 * requiring a re-lookup of the same key. Over-capacity inserts evict after
 * sweeping expired entries first.
 */
public final class InProcessTtlCache<K, V> {

    private final long ttlSeconds;
    private final int maxSize;
    private final ConcurrentHashMap<K, CachedEntry<V>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Object> loadLocks = new ConcurrentHashMap<>();

    public InProcessTtlCache(long ttlSeconds, int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be >= 1");
        }
        this.ttlSeconds = ttlSeconds;
        this.maxSize = maxSize;
    }

    public V get(K key, Supplier<V> loader) {
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
                putEntry(key, value);
                return value;
            } finally {
                loadLocks.remove(key, lock);
            }
        }
    }

    /**
     * Returns a live value if present, otherwise {@code null}. Removes an expired entry.
     */
    public V getIfPresent(K key) {
        CachedEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    public void put(K key, V value) {
        if (ttlSeconds <= 0 || value == null) {
            return;
        }
        putEntry(key, value);
    }

    public void invalidate(K key) {
        cache.remove(key);
    }

    public void sweepExpired() {
        for (Iterator<Map.Entry<K, CachedEntry<V>>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<K, CachedEntry<V>> e = it.next();
            if (e.getValue().isExpired()) {
                it.remove();
            }
        }
    }

    public int size() {
        return cache.size();
    }

    private void putEntry(K key, V value) {
        cache.put(key, new CachedEntry<>(value, Instant.now().plusSeconds(Math.max(ttlSeconds, 0))));
        enforceMaxSize(key);
    }

    private void enforceMaxSize(K keepKey) {
        if (cache.size() <= maxSize) {
            return;
        }
        sweepExpired();
        while (cache.size() > maxSize) {
            K victim = null;
            for (K candidate : cache.keySet()) {
                if (!candidate.equals(keepKey)) {
                    victim = candidate;
                    break;
                }
            }
            if (victim == null) {
                return;
            }
            cache.remove(victim);
        }
    }

    private record CachedEntry<V>(V value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
