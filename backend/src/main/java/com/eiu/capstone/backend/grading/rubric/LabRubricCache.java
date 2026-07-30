package com.eiu.capstone.backend.grading.rubric;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.model.Lab;

@Component
public class LabRubricCache {

    private final LabRubricService labRubricService;
    private final long ttlMinutes;
    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> loadLocks = new ConcurrentHashMap<>();

    public LabRubricCache(LabRubricService labRubricService,
                          @Value("${app.grading.rubric-cache-ttl-minutes:30}") long ttlMinutes) {
        this.labRubricService = labRubricService;
        this.ttlMinutes = ttlMinutes;
    }

    public LabRubricSnapshot get(Lab lab) {
        UUID labId = lab.getId();
        CachedEntry entry = cache.get(labId);
        if (entry != null) {
            if (!entry.isExpired()) {
                return entry.snapshot();
            }
            cache.remove(labId, entry);
        }

        synchronized (loadLocks.computeIfAbsent(labId, ignored -> new Object())) {
            entry = cache.get(labId);
            if (entry != null && !entry.isExpired()) {
                return entry.snapshot();
            }
            LabRubricSnapshot snapshot = labRubricService.loadForLab(lab);
            cache.put(labId, new CachedEntry(snapshot, Instant.now().plusSeconds(ttlMinutes * 60)));
            return snapshot;
        }
    }

    /**
     * Call when rubric rows for a lab change (challenge/class/field/method/constructor writes).
     * TTL remains a fallback when mutation paths do not invoke this yet.
     */
    public void invalidate(UUID labId) {
        cache.remove(labId);
        loadLocks.remove(labId);
    }

    private record CachedEntry(LabRubricSnapshot snapshot, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
