package com.eiu.capstone.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * In-process rolling-window request budget. Multi-instance deploys enforce independently.
 */
@Service
@Profile("!desktop")
public class BurstBudgetService {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final ConcurrentHashMap<String, Bucket> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxWrites;
    private final int maxReads;
    private final Duration window;
    private final int maxKeys;
    private final long defaultRetryAfterSeconds;

    @Autowired
    public BurstBudgetService(
            @Value("${app.burst.max-requests:20}") int maxWrites,
            @Value("${app.burst.max-reads:60}") int maxReads,
            @Value("${app.burst.window-seconds:10}") int windowSeconds,
            @Value("${app.burst.retry-after-seconds:5}") long defaultRetryAfterSeconds,
            @Value("${app.burst.max-keys:10000}") int maxKeys) {
        this(Clock.systemUTC(), maxWrites, maxReads, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    private BurstBudgetService(
            Clock clock,
            int maxWrites,
            int maxReads,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        this.clock = clock;
        this.maxWrites = Math.max(1, maxWrites);
        this.maxReads = Math.max(1, maxReads);
        this.window = Duration.ofSeconds(Math.max(1, windowSeconds));
        this.defaultRetryAfterSeconds = Math.max(1, defaultRetryAfterSeconds);
        this.maxKeys = Math.max(16, maxKeys);
    }

    public static BurstBudgetService forTest(
            Clock clock,
            int maxRequests,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        return forTest(clock, maxRequests, maxRequests, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    public static BurstBudgetService forTest(
            Clock clock,
            int maxWrites,
            int maxReads,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        return new BurstBudgetService(clock, maxWrites, maxReads, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    /** Mutating-request budget (POST, PUT, PATCH, DELETE). */
    public int maxRequests() {
        return maxWrites;
    }

    public int maxReads() {
        return maxReads;
    }

    public long windowSeconds() {
        return window.getSeconds();
    }

    public long defaultRetryAfterSeconds() {
        return defaultRetryAfterSeconds;
    }

    /**
     * Atomically allow or deny one mutating request for {@code key}.
     * Denied requests do not append a timestamp.
     */
    public Decision tryConsume(String key) {
        return tryConsume(key, false);
    }

    /**
     * Reads and writes use separate rolling budgets so a page-load fan-out of GETs
     * does not spend the mutating-request budget (uploads, auth, saves).
     */
    public Decision tryConsume(String key, boolean read) {
        if (key == null || key.isBlank()) {
            return new Decision(true, 0);
        }
        AtomicBoolean allowed = new AtomicBoolean(false);
        AtomicLong retryAfter = new AtomicLong(0);
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        int limit = read ? maxReads : maxWrites;

        windows.compute(key, (k, existing) -> {
            Bucket bucket = existing != null ? existing : new Bucket();
            Deque<Instant> stamps = read ? bucket.reads : bucket.writes;
            while (!stamps.isEmpty() && stamps.peekFirst().isBefore(cutoff)) {
                stamps.pollFirst();
            }
            if (stamps.size() >= limit) {
                allowed.set(false);
                Instant oldest = stamps.peekFirst();
                long untilOpen = Duration.between(now, oldest.plus(window)).getSeconds();
                long retry = Math.max(defaultRetryAfterSeconds, Math.max(1, untilOpen));
                retryAfter.set(retry);
                return bucket.isEmpty() ? null : bucket;
            }
            stamps.addLast(now);
            allowed.set(true);
            return bucket;
        });

        if (allowed.get()) {
            evictIfOverCapacity();
            return new Decision(true, 0);
        }
        return new Decision(false, retryAfter.get());
    }

    private void evictIfOverCapacity() {
        if (windows.size() <= maxKeys) {
            return;
        }
        Instant cutoff = clock.instant().minus(window);
        Iterator<Map.Entry<String, Bucket>> it = windows.entrySet().iterator();
        while (it.hasNext() && windows.size() > maxKeys) {
            Map.Entry<String, Bucket> entry = it.next();
            Bucket bucket = entry.getValue();
            if (bucket == null || bucket.isIdle(cutoff)) {
                it.remove();
            }
        }
        while (windows.size() > maxKeys) {
            String victim = windows.keys().nextElement();
            windows.remove(victim);
        }
    }

    /** Test helper: current in-window mutating-request count for a key. */
    public int currentCount(String key) {
        return currentCount(key, false);
    }

    public int currentCount(String key, boolean read) {
        Bucket bucket = windows.get(key);
        if (bucket == null) {
            return 0;
        }
        Deque<Instant> stamps = read ? bucket.reads : bucket.writes;
        Instant cutoff = clock.instant().minus(window);
        int count = 0;
        for (Instant stamp : stamps) {
            if (!stamp.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    private static final class Bucket {
        private final Deque<Instant> writes = new ArrayDeque<>();
        private final Deque<Instant> reads = new ArrayDeque<>();

        private boolean isEmpty() {
            return writes.isEmpty() && reads.isEmpty();
        }

        private boolean isIdle(Instant cutoff) {
            if (isEmpty()) {
                return true;
            }
            Instant newestWrite = writes.peekLast();
            Instant newestRead = reads.peekLast();
            boolean writesIdle = newestWrite == null || newestWrite.isBefore(cutoff);
            boolean readsIdle = newestRead == null || newestRead.isBefore(cutoff);
            return writesIdle && readsIdle;
        }
    }

    public int keyCount() {
        return windows.size();
    }
}
