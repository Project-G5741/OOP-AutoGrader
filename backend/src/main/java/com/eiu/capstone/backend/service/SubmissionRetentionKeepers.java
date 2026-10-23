package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.eiu.capstone.backend.model.LabSubmission;

/**
 * Keep the {@code maxKeep} newest attempts per student (all labs) and the
 * highest-scoring attempt for each lab. Everything else is eligible to delete.
 */
public final class SubmissionRetentionKeepers {

    private static final Comparator<LabSubmission> RECENCY = Comparator
            .comparing(LabSubmission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getAttemptNumber, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<LabSubmission> BEST_IN_LAB = Comparator
            .comparing(SubmissionRetentionKeepers::score, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getAttemptNumber, Comparator.nullsLast(Comparator.reverseOrder()));

    private SubmissionRetentionKeepers() {}

    public static List<UUID> idsToPrune(List<LabSubmission> submissions, int maxKeep) {
        if (submissions == null || submissions.isEmpty() || maxKeep < 1) {
            return List.of();
        }
        Set<UUID> keep = new HashSet<>();
        List<LabSubmission> byRecency = new ArrayList<>(submissions);
        byRecency.sort(RECENCY);
        int newestCount = Math.min(maxKeep, byRecency.size());
        for (int i = 0; i < newestCount; i++) {
            UUID id = byRecency.get(i).getId();
            if (id != null) {
                keep.add(id);
            }
        }
        Map<UUID, LabSubmission> bestByLab = new HashMap<>();
        for (LabSubmission submission : submissions) {
            if (submission.getLab() == null || submission.getLab().getId() == null) {
                continue;
            }
            UUID labId = submission.getLab().getId();
            LabSubmission current = bestByLab.get(labId);
            if (current == null || BEST_IN_LAB.compare(submission, current) < 0) {
                bestByLab.put(labId, submission);
            }
        }
        for (LabSubmission best : bestByLab.values()) {
            if (best.getId() != null) {
                keep.add(best.getId());
            }
        }
        List<UUID> toDelete = new ArrayList<>();
        for (LabSubmission submission : submissions) {
            UUID id = submission.getId();
            if (id != null && !keep.contains(id)) {
                toDelete.add(id);
            }
        }
        return toDelete;
    }

    private static BigDecimal score(LabSubmission submission) {
        return submission.getScore();
    }
}
