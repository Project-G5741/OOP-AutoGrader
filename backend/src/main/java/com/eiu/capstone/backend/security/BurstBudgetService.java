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

    public enum Lane {
        READ,
        WRITE,
        /** Public /api/auth/** — higher IP budget for shared campus Wi‑Fi login bursts. */
        AUTH
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final ConcurrentHashMap<String, Bucket> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxWrites;
    private final int maxReads;
    private final int maxAuth;
    private final Duration window;
    private final int maxKeys;
    private final long defaultRetryAfterSeconds;

    @Autowired
    public BurstBudgetService(
            @Value("${app.burst.max-requests:20}") int maxWrites,
            @Value("${app.burst.max-reads:60}") int maxReads,
            @Value("${app.burst.max-auth-requests:60}") int maxAuth,
            @Value("${app.burst.window-seconds:10}") int windowSeconds,
            @Value("${app.burst.retry-after-seconds:5}") long defaultRetryAfterSeconds,
            @Value("${app.burst.max-keys:10000}") int maxKeys) {
        this(Clock.systemUTC(), maxWrites, maxReads, maxAuth, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    private BurstBudgetService(
            Clock clock,
            int maxWrites,
            int maxReads,
            int maxAuth,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        this.clock = clock;
        this.maxWrites = Math.max(1, maxWrites);
        this.maxReads = Math.max(1, maxReads);
        this.maxAuth = Math.max(1, maxAuth);
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
        return forTest(clock, maxRequests, maxRequests, maxRequests, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    public static BurstBudgetService forTest(
            Clock clock,
            int maxWrites,
            int maxReads,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        return forTest(clock, maxWrites, maxReads, maxWrites, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    public static BurstBudgetService forTest(
            Clock clock,
            int maxWrites,
            int maxReads,
            int maxAuth,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        return new BurstBudgetService(
                clock, maxWrites, maxReads, maxAuth, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    /** Mutating-request budget (POST, PUT, PATCH, DELETE) outside /api/auth. */
    public int maxRequests() {
        return maxWrites;
    }

    public int maxReads() {
        return maxReads;
    }

    public int maxAuth() {
        return maxAuth;
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
        return tryConsume(key, Lane.WRITE);
    }

    /**
     * Reads and writes use separate rolling budgets so a page-load fan-out of GETs
     * does not spend the mutating-request budget (uploads, saves).
     */
    public Decision tryConsume(String key, boolean read) {
        return tryConsume(key, read ? Lane.READ : Lane.WRITE);
    }

    /**
     * Auth lane is separate from ordinary writes so a lab of students sharing one
     * NAT/Wi‑Fi IP can log in together without burning the tight write budget.
     */
    public Decision tryConsume(String key, Lane lane) {
        if (key == null || key.isBlank()) {
            return new Decision(true, 0);
        }
        Lane effective = lane == null ? Lane.WRITE : lane;
        AtomicBoolean allowed = new AtomicBoolean(false);
        AtomicLong retryAfter = new AtomicLong(0);
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        int limit = limitFor(effective);

        windows.compute(key, (k, existing) -> {
            Bucket bucket = existing != null ? existing : new Bucket();
            Deque<Instant> stamps = stampsFor(bucket, effective);
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

    private int limitFor(Lane lane) {
        return switch (lane) {
            case READ -> maxReads;
            case AUTH -> maxAuth;
            case WRITE -> maxWrites;
        };
    }

    private static Deque<Instant> stampsFor(Bucket bucket, Lane lane) {
        return switch (lane) {
            case READ -> bucket.reads;
            case AUTH -> bucket.auths;
            case WRITE -> bucket.writes;
        };
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
        return currentCount(key, Lane.WRITE);
    }

    public int currentCount(String key, boolean read) {
        return currentCount(key, read ? Lane.READ : Lane.WRITE);
    }

    public int currentCount(String key, Lane lane) {
        Bucket bucket = windows.get(key);
        if (bucket == null) {
            return 0;
        }
        Deque<Instant> stamps = stampsFor(bucket, lane == null ? Lane.WRITE : lane);
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
        private final Deque<Instant> auths = new ArrayDeque<>();

        private boolean isEmpty() {
            return writes.isEmpty() && reads.isEmpty() && auths.isEmpty();
        }

        private boolean isIdle(Instant cutoff) {
            if (isEmpty()) {
                return true;
            }
            Instant newestWrite = writes.peekLast();
            Instant newestRead = reads.peekLast();
            Instant newestAuth = auths.peekLast();
            boolean writesIdle = newestWrite == null || newestWrite.isBefore(cutoff);
            boolean readsIdle = newestRead == null || newestRead.isBefore(cutoff);
            boolean authsIdle = newestAuth == null || newestAuth.isBefore(cutoff);
            return writesIdle && readsIdle && authsIdle;
        }
    }

    public int keyCount() {
        return windows.size();
    }
}
