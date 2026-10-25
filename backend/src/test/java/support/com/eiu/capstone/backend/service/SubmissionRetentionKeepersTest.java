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
    void latestOnTimeIsHighest_keepsOnlyThatRow() {
        LabSubmission latestBest = row(labA, 3, "2026-10-20T10:00:00Z", "90");
        List<LabSubmission> rows = List.of(
                latestBest,
                row(labA, 2, "2026-10-19T00:00:00Z", "40"),
                row(labA, 1, "2026-10-18T00:00:00Z", "10"));

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));

        assertEquals(Set.of(rows.get(1).getId(), rows.get(2).getId()), deleted);
    }

    @Test
    void olderOnTimeHighest_keptWithLatestOnTime() {
        LabSubmission latest = row(labA, 3, "2026-10-20T10:00:00Z", "10");
        LabSubmission highest = row(labA, 1, "2026-10-18T00:00:00Z", "90");
        List<LabSubmission> rows = List.of(
                latest,
                row(labA, 2, "2026-10-19T00:00:00Z", "40"),
                highest);

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));

        assertEquals(Set.of(rows.get(1).getId()), deleted);
        assertTrue(deleted.stream().noneMatch(id -> id.equals(latest.getId())));
        assertTrue(deleted.stream().noneMatch(id -> id.equals(highest.getId())));
    }

    @Test
    void lateRowsAreAlwaysDeleted_evenIfLatestOrHighest() {
        LabSubmission lateLatest = row(labA, 4, "2026-10-24T00:00:00Z", "10", deadline);
        LabSubmission lateHighest = row(labA, 3, "2026-10-23T00:00:00Z", "99", deadline);
        LabSubmission onTimeHighest = row(labA, 1, "2026-10-20T10:00:00Z", "74", deadline);
        LabSubmission onTimeLatest = row(labA, 2, "2026-10-20T16:00:00Z", "50", deadline);
        List<LabSubmission> rows = List.of(lateLatest, lateHighest, onTimeLatest, onTimeHighest);

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));

        assertTrue(deleted.contains(lateLatest.getId()));
        assertTrue(deleted.contains(lateHighest.getId()));
        assertTrue(deleted.stream().noneMatch(id -> id.equals(onTimeHighest.getId())));
        assertTrue(deleted.stream().noneMatch(id -> id.equals(onTimeLatest.getId())));
    }

    @Test
    void allLate_deletesEverything() {
        List<LabSubmission> rows = List.of(
                row(labA, 2, "2026-10-22T00:00:00Z", "90", deadline),
                row(labA, 1, "2026-10-21T00:00:00Z", "40", deadline));

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));
        assertEquals(Set.of(rows.get(0).getId(), rows.get(1).getId()), deleted);
    }

    @Test
    void eachLabKeepsItsOwnOnTimeHighest() {
        LabSubmission labBHighest = row(labB, 1, "2026-09-01T00:00:00Z", "100");
        List<LabSubmission> rows = new ArrayList<>();
        for (int attempt = 8; attempt >= 1; attempt--) {
            rows.add(row(labA, attempt, "2026-10-" + String.format("%02d", attempt) + "T00:00:00Z", "1"));
        }
        rows.add(labBHighest);
        rows.add(row(labB, 2, "2026-09-02T00:00:00Z", "20"));

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows));

        assertTrue(deleted.stream().noneMatch(id -> id.equals(labBHighest.getId())));
        LabSubmission labALatest = rows.get(0);
        assertTrue(deleted.stream().noneMatch(id -> id.equals(labALatest.getId())));
    }

    @Test
    void emptyOrSingleOnTime_noDeletes() {
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of()).isEmpty());
        LabSubmission only = row(labA, 1, "2026-10-01T00:00:00Z", "50");
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of(only)).isEmpty());
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
