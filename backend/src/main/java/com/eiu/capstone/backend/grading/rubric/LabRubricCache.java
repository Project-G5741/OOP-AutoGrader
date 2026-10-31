package com.eiu.capstone.backend.grading.rubric;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.repository.LabRepository;

@Component
public class LabRubricCache {

    private final LabRubricService labRubricService;
    private final LabRepository labRepository;
    private final long ttlMinutes;
    private final int maxSize;
    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> loadLocks = new ConcurrentHashMap<>();

    @Autowired
    public LabRubricCache(LabRubricService labRubricService,
                          LabRepository labRepository,
                          @Value("${app.grading.rubric-cache-ttl-minutes:2}") long ttlMinutes,
                          @Value("${app.grading.rubric-cache-max-size:8}") int maxSize) {
        this.labRubricService = labRubricService;
        this.labRepository = labRepository;
        this.ttlMinutes = ttlMinutes;
        this.maxSize = Math.max(1, maxSize);
    }

    /** Test constructor with default max size. */
    public LabRubricCache(LabRubricService labRubricService,
                          LabRepository labRepository,
                          long ttlMinutes) {
        this(labRubricService, labRepository, ttlMinutes, 8);
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
            putEntry(labId, snapshot);
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
            for (Lab lab : misses) {
                UUID labId = lab.getId();
                synchronized (loadLocks.computeIfAbsent(labId, ignored -> new Object())) {
                    CachedEntry existing = cache.get(labId);
                    if (existing != null && !existing.isExpired()) {
                        result.put(labId, existing.snapshot());
                        continue;
                    }
                    LabRubricSnapshot snapshot = loaded.getOrDefault(labId, new LabRubricSnapshot(labId, Map.of()));
                    putEntry(labId, snapshot);
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

    /** Remove expired entries without requiring a same-key re-hit. */
    public void sweepExpired() {
        for (Iterator<Map.Entry<UUID, CachedEntry>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, CachedEntry> e = it.next();
            if (e.getValue().isExpired()) {
                it.remove();
                loadLocks.remove(e.getKey());
            }
        }
    }

    public int size() {
        return cache.size();
    }

    private void putEntry(UUID labId, LabRubricSnapshot snapshot) {
        cache.put(labId, new CachedEntry(snapshot, Instant.now().plusSeconds(ttlMinutes * 60)));
        enforceMaxSize(labId);
    }

    private void enforceMaxSize(UUID keepKey) {
        if (cache.size() <= maxSize) {
            return;
        }
        sweepExpired();
        while (cache.size() > maxSize) {
            UUID victim = null;
            for (UUID candidate : cache.keySet()) {
                if (!candidate.equals(keepKey)) {
                    victim = candidate;
                    break;
                }
            }
            if (victim == null) {
                return;
            }
            cache.remove(victim);
            loadLocks.remove(victim);
        }
    }

    private record CachedEntry(LabRubricSnapshot snapshot, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
