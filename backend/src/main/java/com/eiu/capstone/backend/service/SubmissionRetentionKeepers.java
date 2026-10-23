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
 * Row keepers: {@code maxKeep} newest attempts per student (all labs) plus each lab's
 * highest-scoring attempt. Detail keepers: latest attempt per lab plus that lab's highest.
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
        Set<UUID> keep = newestIds(submissions, maxKeep);
        keep.addAll(idsOf(bestByLab(submissions).values()));
        return missingFrom(submissions, keep);
    }

    /**
     * Among remaining score rows, drop Class/MMD/OT ticks everywhere except latest + highest per lab.
     */
    public static List<UUID> idsToStripDetails(List<LabSubmission> remaining) {
        if (remaining == null || remaining.isEmpty()) {
            return List.of();
        }
        Set<UUID> keepDetail = new HashSet<>();
        keepDetail.addAll(idsOf(latestByLab(remaining).values()));
        keepDetail.addAll(idsOf(bestByLab(remaining).values()));
        return missingFrom(remaining, keepDetail);
    }

    private static Set<UUID> newestIds(List<LabSubmission> submissions, int maxKeep) {
        List<LabSubmission> byRecency = new ArrayList<>(submissions);
        byRecency.sort(RECENCY);
        Set<UUID> keep = new HashSet<>();
        int newestCount = Math.min(maxKeep, byRecency.size());
        for (int i = 0; i < newestCount; i++) {
            UUID id = byRecency.get(i).getId();
            if (id != null) {
                keep.add(id);
            }
        }
        return keep;
    }

    private static Map<UUID, LabSubmission> latestByLab(List<LabSubmission> submissions) {
        Map<UUID, LabSubmission> latest = new HashMap<>();
        for (LabSubmission submission : submissions) {
            UUID labId = labId(submission);
            if (labId == null) {
                continue;
            }
            LabSubmission current = latest.get(labId);
            if (current == null || RECENCY.compare(submission, current) < 0) {
                latest.put(labId, submission);
            }
        }
        return latest;
    }

    private static Map<UUID, LabSubmission> bestByLab(List<LabSubmission> submissions) {
        Map<UUID, LabSubmission> best = new HashMap<>();
        for (LabSubmission submission : submissions) {
            UUID labId = labId(submission);
            if (labId == null) {
                continue;
            }
            LabSubmission current = best.get(labId);
            if (current == null || BEST_IN_LAB.compare(submission, current) < 0) {
                best.put(labId, submission);
            }
        }
        return best;
    }

    private static List<UUID> missingFrom(List<LabSubmission> submissions, Set<UUID> keep) {
        List<UUID> extra = new ArrayList<>();
        for (LabSubmission submission : submissions) {
            UUID id = submission.getId();
            if (id != null && !keep.contains(id)) {
                extra.add(id);
            }
        }
        return extra;
    }

    private static Set<UUID> idsOf(Iterable<LabSubmission> submissions) {
        Set<UUID> ids = new HashSet<>();
        for (LabSubmission submission : submissions) {
            if (submission.getId() != null) {
                ids.add(submission.getId());
            }
        }
        return ids;
    }

    private static UUID labId(LabSubmission submission) {
        if (submission.getLab() == null) {
            return null;
        }
        return submission.getLab().getId();
    }

    private static BigDecimal score(LabSubmission submission) {
        return submission.getScore();
    }
}
