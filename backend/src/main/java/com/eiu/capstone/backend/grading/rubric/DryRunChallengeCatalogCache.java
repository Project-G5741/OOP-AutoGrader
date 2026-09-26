package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Short-lived per-challenge catalogs for lecturer dry-run validate + assemble.
 * Entries remember the lab they were loaded for so warm hits need no Neon round-trip
 * but still reject a mismatched labId. Cleared on any lab rubric invalidation.
 */
@Component
public class DryRunChallengeCatalogCache {

    private static final long TTL_MS = 60_000L;

    private final ConcurrentHashMap<UUID, Entry> memberIdsByChallenge = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Entry> memberMapsByChallenge = new ConcurrentHashMap<>();

    public interface Loader<T> {
        T load();
    }

    public <T> T getMemberIds(UUID labId, UUID challengeId, Loader<T> loader) {
        return getOrLoad(memberIdsByChallenge, labId, challengeId, loader);
    }

    public <T> T getMemberMaps(UUID labId, UUID challengeId, Loader<T> loader) {
        return getOrLoad(memberMapsByChallenge, labId, challengeId, loader);
    }

    public boolean hasWarmCatalog(UUID labId, UUID challengeId) {
        return isWarm(memberIdsByChallenge, labId, challengeId)
                && isWarm(memberMapsByChallenge, labId, challengeId);
    }

    public void invalidateAll() {
        memberIdsByChallenge.clear();
        memberMapsByChallenge.clear();
    }

    private static boolean isWarm(ConcurrentHashMap<UUID, Entry> map, UUID labId, UUID challengeId) {
        if (challengeId == null || labId == null) {
            return false;
        }
        Entry entry = map.get(challengeId);
        return entry != null
                && System.currentTimeMillis() < entry.deadlineMs
                && labId.equals(entry.labId);
    }

    private static <T> T getOrLoad(ConcurrentHashMap<UUID, Entry> map,
                                   UUID labId,
                                   UUID challengeId,
                                   Loader<T> loader) {
        if (challengeId == null) {
            return loader.load();
        }
        Entry entry = map.get(challengeId);
        long now = System.currentTimeMillis();
        if (entry != null && now < entry.deadlineMs) {
            if (labId != null && !labId.equals(entry.labId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found in lab");
            }
            @SuppressWarnings("unchecked")
            T value = (T) entry.value;
            return value;
        }
        T loaded = loader.load();
        map.put(challengeId, new Entry(loaded, labId, now + TTL_MS));
        return loaded;
    }

    private record Entry(Object value, UUID labId, long deadlineMs) {}
}
