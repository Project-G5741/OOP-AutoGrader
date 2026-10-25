package com.eiu.capstone.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Per lab keep only on-time attempts: the latest before the deadline and the
 * highest on-time score. Late rows are always pruned. One row when they match.
 */
public final class SubmissionRetentionKeepers {

    private static final LabDeadlineHelper DEADLINE = new LabDeadlineHelper();

    private static final Comparator<LabSubmission> RECENCY = Comparator
            .comparing(LabSubmission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getAttemptNumber, Comparator.nullsLast(Comparator.reverseOrder()));

    private static final Comparator<LabSubmission> BEST_IN_LAB = Comparator
            .comparing(SubmissionRetentionKeepers::score, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LabSubmission::getAttemptNumber, Comparator.nullsLast(Comparator.reverseOrder()));

    private SubmissionRetentionKeepers() {}

    public static List<UUID> idsToPrune(List<LabSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        Set<UUID> keep = idsOf(latestOnTimeByLab(submissions).values());
        keep.addAll(idsOf(bestOnTimeByLab(submissions).values()));
        return missingFrom(submissions, keep);
    }

    private static Map<UUID, LabSubmission> latestOnTimeByLab(List<LabSubmission> submissions) {
        Map<UUID, LabSubmission> latest = new HashMap<>();
        for (LabSubmission submission : submissions) {
            UUID labId = labId(submission);
            if (labId == null || !countsForLecturer(submission)) {
                continue;
            }
            LabSubmission current = latest.get(labId);
            if (current == null || RECENCY.compare(submission, current) < 0) {
                latest.put(labId, submission);
            }
        }
        return latest;
    }

    private static Map<UUID, LabSubmission> bestOnTimeByLab(List<LabSubmission> submissions) {
        Map<UUID, LabSubmission> best = new HashMap<>();
        for (LabSubmission submission : submissions) {
            UUID labId = labId(submission);
            if (labId == null || !countsForLecturer(submission)) {
                continue;
            }
            LabSubmission current = best.get(labId);
            if (current == null || BEST_IN_LAB.compare(submission, current) < 0) {
                best.put(labId, submission);
            }
        }
        return best;
    }

    static boolean countsForLecturer(LabSubmission submission) {
        if (submission == null) {
            return false;
        }
        LocalDate deadline = submission.getLab() == null ? null : submission.getLab().getDeadlineDate();
        return DEADLINE.submissionCountsForLecturer(deadline, submission.getSubmittedAt());
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
