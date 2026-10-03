package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Short-lived per-challenge catalogs for lecturer dry-run validate + assemble.
 * One Neon load populates both validation membership and assemble member maps.
 * Entries remember the lab they were loaded for so warm hits need no Neon round-trip
 * but still reject a mismatched labId. Cleared on any lab rubric invalidation.
 */
@Component
public class DryRunChallengeCatalogCache {

    private static final long TTL_MS = 60_000L;

    private final ConcurrentHashMap<UUID, WarmEntry> catalogByChallenge = new ConcurrentHashMap<>();

    public interface Loader {
        CatalogEntry load();
    }

    public record CatalogEntry(Object memberIds, RubricMemberMaps memberMaps) {}

    public CatalogEntry getCatalog(UUID labId, UUID challengeId, Loader loader) {
        if (challengeId == null) {
            return loader.load();
        }
        WarmEntry entry = catalogByChallenge.get(challengeId);
        long now = System.currentTimeMillis();
        if (entry != null && now < entry.deadlineMs) {
            if (labId != null && !labId.equals(entry.labId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found in lab");
            }
            return entry.catalog;
        }
        CatalogEntry loaded = loader.load();
        catalogByChallenge.put(challengeId, new WarmEntry(loaded, labId, now + TTL_MS));
        return loaded;
    }

    public boolean hasWarmCatalog(UUID labId, UUID challengeId) {
        if (challengeId == null || labId == null) {
            return false;
        }
        WarmEntry entry = catalogByChallenge.get(challengeId);
        return entry != null
                && System.currentTimeMillis() < entry.deadlineMs
                && labId.equals(entry.labId);
    }

    public void invalidateAll() {
        catalogByChallenge.clear();
    }

    private record WarmEntry(CatalogEntry catalog, UUID labId, long deadlineMs) {}
}
