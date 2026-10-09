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

    private final ConcurrentHashMap<String, Deque<Instant>> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxRequests;
    private final Duration window;
    private final int maxKeys;
    private final long defaultRetryAfterSeconds;

    @Autowired
    public BurstBudgetService(
            @Value("${app.burst.max-requests:20}") int maxRequests,
            @Value("${app.burst.window-seconds:10}") int windowSeconds,
            @Value("${app.burst.retry-after-seconds:5}") long defaultRetryAfterSeconds,
            @Value("${app.burst.max-keys:10000}") int maxKeys) {
        this(Clock.systemUTC(), maxRequests, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    private BurstBudgetService(
            Clock clock,
            int maxRequests,
            int windowSeconds,
            long defaultRetryAfterSeconds,
            int maxKeys) {
        this.clock = clock;
        this.maxRequests = Math.max(1, maxRequests);
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
        return new BurstBudgetService(clock, maxRequests, windowSeconds, defaultRetryAfterSeconds, maxKeys);
    }

    public int maxRequests() {
        return maxRequests;
    }

    public long windowSeconds() {
        return window.getSeconds();
    }

    public long defaultRetryAfterSeconds() {
        return defaultRetryAfterSeconds;
    }

    /**
     * Atomically allow or deny one request for {@code key}. Denied requests do not append a timestamp.
     */
    public Decision tryConsume(String key) {
        if (key == null || key.isBlank()) {
            return new Decision(true, 0);
        }
        AtomicBoolean allowed = new AtomicBoolean(false);
        AtomicLong retryAfter = new AtomicLong(0);
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);

        windows.compute(key, (k, deque) -> {
            Deque<Instant> stamps = deque != null ? deque : new ArrayDeque<>();
            while (!stamps.isEmpty() && stamps.peekFirst().isBefore(cutoff)) {
                stamps.pollFirst();
            }
            if (stamps.size() >= maxRequests) {
                allowed.set(false);
                Instant oldest = stamps.peekFirst();
                long untilOpen = Duration.between(now, oldest.plus(window)).getSeconds();
                long retry = Math.max(defaultRetryAfterSeconds, Math.max(1, untilOpen));
                retryAfter.set(retry);
                return stamps.isEmpty() ? null : stamps;
            }
            stamps.addLast(now);
            allowed.set(true);
            return stamps;
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
        Iterator<Map.Entry<String, Deque<Instant>>> it = windows.entrySet().iterator();
        while (it.hasNext() && windows.size() > maxKeys) {
            Map.Entry<String, Deque<Instant>> entry = it.next();
            Deque<Instant> stamps = entry.getValue();
            if (stamps == null || stamps.isEmpty() || stamps.peekLast().isBefore(cutoff)) {
                it.remove();
            }
        }
        while (windows.size() > maxKeys) {
            String victim = windows.keys().nextElement();
            windows.remove(victim);
        }
    }

    /** Test helper: current in-window count for a key. */
    public int currentCount(String key) {
        Deque<Instant> stamps = windows.get(key);
        if (stamps == null) {
            return 0;
        }
        Instant cutoff = clock.instant().minus(window);
        int count = 0;
        for (Instant stamp : stamps) {
            if (!stamp.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    public int keyCount() {
        return windows.size();
    }
}
