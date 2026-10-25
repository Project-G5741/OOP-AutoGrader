package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.service.SubmissionRetentionKeepers;

class SubmissionRetentionKeepersTest {

    private final UUID labA = UUID.randomUUID();
    private final UUID labB = UUID.randomUUID();
    private final LocalDate deadline = LocalDate.of(2026, 10, 20);

    @Test
    void latestOnTimeIsHighest_keepsAllOnTimeRows_stripsOlderDetails() {
        LabSubmission latestBest = row(labA, 3, "2026-10-20T10:00:00Z", "90");
        List<LabSubmission> rows = List.of(
                latestBest,
                row(labA, 2, "2026-10-19T00:00:00Z", "40"),
                row(labA, 1, "2026-10-18T00:00:00Z", "10"));

        assertTrue(SubmissionRetentionKeepers.idsToPrune(rows).isEmpty());
        assertEquals(Set.of(rows.get(1).getId(), rows.get(2).getId()),
                Set.copyOf(SubmissionRetentionKeepers.idsToStrip(rows)));
    }

    @Test
    void olderOnTimeHighest_keepsFullDetailsWithLatest_stripsMiddle() {
        LabSubmission latest = row(labA, 3, "2026-10-20T10:00:00Z", "10");
        LabSubmission highest = row(labA, 1, "2026-10-18T00:00:00Z", "90");
        List<LabSubmission> rows = List.of(
                latest,
                row(labA, 2, "2026-10-19T00:00:00Z", "40"),
                highest);

        assertTrue(SubmissionRetentionKeepers.idsToPrune(rows).isEmpty());
        Set<UUID> stripped = Set.copyOf(SubmissionRetentionKeepers.idsToStrip(rows));
        assertEquals(Set.of(rows.get(1).getId()), stripped);
        assertTrue(stripped.stream().noneMatch(id -> id.equals(latest.getId())));
        assertTrue(stripped.stream().noneMatch(id -> id.equals(highest.getId())));
    }

    @Test
    void lateRowsAreAlwaysDeleted_evenIfLatestOrHighest() {
        LabSubmission lateLatest = row(labA, 4, "2026-10-24T00:00:00Z", "10", deadline);
        LabSubmission lateHighest = row(labA, 3, "2026-10-23T00:00:00Z", "99", deadline);
        LabSubmission onTimeHighest = row(labA, 1, "2026-10-20T10:00:00Z", "74", deadline);
        LabSubmission onTimeLatest = row(labA, 2, "2026-10-20T16:00:00Z", "50", deadline);
        List<LabSubmission> rows = List.of(lateLatest, lateHighest, onTimeLatest, onTimeHighest);

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));
        assertEquals(Set.of(lateLatest.getId(), lateHighest.getId()), deleted);
        assertTrue(SubmissionRetentionKeepers.idsToStrip(rows).isEmpty());
    }

    @Test
    void allLate_deletesEverything() {
        List<LabSubmission> rows = List.of(
                row(labA, 2, "2026-10-22T00:00:00Z", "90", deadline),
                row(labA, 1, "2026-10-21T00:00:00Z", "40", deadline));

        assertEquals(Set.of(rows.get(0).getId(), rows.get(1).getId()),
                Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows)));
        assertTrue(SubmissionRetentionKeepers.idsToStrip(rows).isEmpty());
    }

    @Test
    void eachLabKeepsItsOwnOnTimeHighestDetails() {
        LabSubmission labBHighest = row(labB, 1, "2026-09-01T00:00:00Z", "100");
        List<LabSubmission> rows = new ArrayList<>();
        for (int attempt = 8; attempt >= 1; attempt--) {
            rows.add(row(labA, attempt, "2026-10-" + String.format("%02d", attempt) + "T00:00:00Z", "1"));
        }
        rows.add(labBHighest);
        rows.add(row(labB, 2, "2026-09-02T00:00:00Z", "20"));

        assertTrue(SubmissionRetentionKeepers.idsToPrune(rows).isEmpty());
        Set<UUID> stripped = Set.copyOf(SubmissionRetentionKeepers.idsToStrip(rows));
        assertTrue(stripped.stream().noneMatch(id -> id.equals(labBHighest.getId())));
        LabSubmission labALatest = rows.get(0);
        assertTrue(stripped.stream().noneMatch(id -> id.equals(labALatest.getId())));
        assertEquals(7, stripped.size());
    }

    @Test
    void emptyOrSingleOnTime_noDeletesOrStrips() {
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of()).isEmpty());
        assertTrue(SubmissionRetentionKeepers.idsToStrip(List.of()).isEmpty());
        LabSubmission only = row(labA, 1, "2026-10-01T00:00:00Z", "50");
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of(only)).isEmpty());
        assertTrue(SubmissionRetentionKeepers.idsToStrip(List.of(only)).isEmpty());
    }

    private static LabSubmission row(UUID labId, int attempt, String submittedAt, String score) {
        return row(labId, attempt, submittedAt, score, null);
    }

    private static LabSubmission row(UUID labId, int attempt, String submittedAt, String score, LocalDate deadlineDate) {
        Lab lab = new Lab();
        lab.setId(labId);
        lab.setDeadlineDate(deadlineDate);
        LabSubmission submission = new LabSubmission();
        submission.setId(UUID.randomUUID());
        submission.setLab(lab);
        submission.setAttemptNumber(attempt);
        submission.setSubmittedAt(OffsetDateTime.parse(submittedAt).withOffsetSameInstant(ZoneOffset.UTC));
        submission.setScore(new BigDecimal(score));
        return submission;
    }
}
