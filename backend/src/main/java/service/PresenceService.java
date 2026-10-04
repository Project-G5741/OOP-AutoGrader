package com.eiu.capstone.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * In-process last-seen map of signed-in users. Multi-instance deploys count independently.
 */
@Service
public class PresenceService {

    public static final Duration ACTIVE_WINDOW = Duration.ofSeconds(30);

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final Clock clock;

    public PresenceService() {
        this(Clock.systemUTC());
    }

    private PresenceService(Clock clock) {
        this.clock = clock;
    }

    public static PresenceService forClock(Clock clock) {
        return new PresenceService(clock);
    }

    public void heartbeat(String email) {
        String key = normalize(email);
        if (key != null) {
            lastSeen.put(key, clock.instant());
        }
    }

    public void leave(String email) {
        String key = normalize(email);
        if (key != null) {
            lastSeen.remove(key);
        }
    }

    public int countActive() {
        Instant cutoff = clock.instant().minus(ACTIVE_WINDOW);
        lastSeen.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        return lastSeen.size();
    }

    private static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
