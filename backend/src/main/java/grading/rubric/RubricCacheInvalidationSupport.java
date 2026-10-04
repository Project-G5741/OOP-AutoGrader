package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Invoked from rubric mutation code paths (admin APIs, entity listeners) to drop stale snapshots.
 */
@Component
public class RubricCacheInvalidationSupport {

    private final LabRubricCache labRubricCache;
    private final DryRunChallengeCatalogCache dryRunChallengeCatalogCache;

    public RubricCacheInvalidationSupport(LabRubricCache labRubricCache,
                                          DryRunChallengeCatalogCache dryRunChallengeCatalogCache) {
        this.labRubricCache = labRubricCache;
        this.dryRunChallengeCatalogCache = dryRunChallengeCatalogCache;
    }

    public void invalidateLab(UUID labId) {
        if (labId != null) {
            labRubricCache.invalidate(labId);
        }
        dryRunChallengeCatalogCache.invalidateAll();
    }
}
