package com.eiu.capstone.backend.grading.rubric;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.LabRepository;

@Component
public class LabRubricCache {

    private final LabRubricService labRubricService;
    private final LabRepository labRepository;
    private final long ttlMinutes;
    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> loadLocks = new ConcurrentHashMap<>();

    public LabRubricCache(LabRubricService labRubricService,
                          LabRepository labRepository,
                          @Value("${app.grading.rubric-cache-ttl-minutes:30}") long ttlMinutes) {
        this.labRubricService = labRubricService;
        this.labRepository = labRepository;
        this.ttlMinutes = ttlMinutes;
    }

    public LabRubricSnapshot get(UUID labId) {
        if (labId == null) {
            throw new IllegalArgumentException("labId is required");
        }
        CachedEntry entry = cache.get(labId);
        if (entry != null && !entry.isExpired()) {
            return entry.snapshot();
        }
        return get(labRepository.getReferenceById(labId));
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
     * Resolve many labs with cache hits first, then one batched {@link LabRubricService#loadForLabs}
     * for all misses (not one load per lab).
     */
    public Map<UUID, LabRubricSnapshot> getAll(Collection<Lab> labs) {
        if (labs == null || labs.isEmpty()) {
            return Map.of();
        }

        Map<UUID, LabRubricSnapshot> result = new LinkedHashMap<>();
        List<Lab> misses = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (Lab lab : labs) {
            if (lab == null || lab.getId() == null) {
                continue;
            }
            UUID labId = lab.getId();
            if (!seen.add(labId)) {
                continue;
            }
            CachedEntry entry = cache.get(labId);
            if (entry != null && !entry.isExpired()) {
                result.put(labId, entry.snapshot());
                continue;
            }
            if (entry != null) {
                cache.remove(labId, entry);
            }
            misses.add(lab);
        }

        if (!misses.isEmpty()) {
            Map<UUID, LabRubricSnapshot> loaded = labRubricService.loadForLabs(misses);
            Instant expiresAt = Instant.now().plusSeconds(ttlMinutes * 60);
            for (Lab lab : misses) {
                UUID labId = lab.getId();
                synchronized (loadLocks.computeIfAbsent(labId, ignored -> new Object())) {
                    CachedEntry existing = cache.get(labId);
                    if (existing != null && !existing.isExpired()) {
                        result.put(labId, existing.snapshot());
                        continue;
                    }
                    LabRubricSnapshot snapshot = loaded.getOrDefault(labId, new LabRubricSnapshot(labId, Map.of()));
                    cache.put(labId, new CachedEntry(snapshot, expiresAt));
                    result.put(labId, snapshot);
                }
            }
        }

        return Map.copyOf(result);
    }

    /**
     * Call when rubric rows for a lab change (challenge/class/field/method/constructor writes).
     * TTL remains a fallback when mutation paths do not invoke this yet.
     */
    public void invalidate(UUID labId) {
        cache.remove(labId);
        loadLocks.remove(labId);
    }

    public void invalidateAll() {
        cache.clear();
        loadLocks.clear();
    }

    private record CachedEntry(LabRubricSnapshot snapshot, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
