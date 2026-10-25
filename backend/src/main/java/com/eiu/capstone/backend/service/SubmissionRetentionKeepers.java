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
 * Per lab, keep every on-time {@code lab_submission} score row. Full ticks stay
 * only on the latest on-time attempt and the highest on-time attempt. Late
 * rows are always deleted.
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

    /** Late attempts — delete the whole graph. */
    public static List<UUID> idsToPrune(List<LabSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        List<UUID> late = new ArrayList<>();
        for (LabSubmission submission : submissions) {
            UUID id = submission.getId();
            if (id != null && !countsForLecturer(submission)) {
                late.add(id);
            }
        }
        return late;
    }

    /**
     * On-time attempts that are neither the lab's latest on-time nor its
     * highest on-time — drop Class/MMD/OT ticks and sidecars, keep the score row.
     */
    public static List<UUID> idsToStrip(List<LabSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }
        Set<UUID> keepDetails = idsOf(latestOnTimeByLab(submissions).values());
        keepDetails.addAll(idsOf(bestOnTimeByLab(submissions).values()));
        List<UUID> strip = new ArrayList<>();
        for (LabSubmission submission : submissions) {
            UUID id = submission.getId();
            if (id != null && countsForLecturer(submission) && !keepDetails.contains(id)) {
                strip.add(id);
            }
        }
        return strip;
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
