package com.eiu.capstone.backend.grading.rubric;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Invoked from rubric mutation code paths (admin APIs, entity listeners) to drop stale snapshots.
 */
@Component
public class RubricCacheInvalidationSupport {

    private final LabRubricCache labRubricCache;

    public RubricCacheInvalidationSupport(LabRubricCache labRubricCache) {
        this.labRubricCache = labRubricCache;
    }

    public void invalidateLab(UUID labId) {
        if (labId != null) {
            labRubricCache.invalidate(labId);
        }
    }
}
