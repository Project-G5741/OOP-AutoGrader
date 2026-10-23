package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    @Test
    void elevenOnOneLab_bestAlreadyInNewestTen_deletesOldest() {
        List<LabSubmission> rows = List.of(
                row(labA, 11, "2026-10-11T00:00:00Z", "40"),
                row(labA, 10, "2026-10-10T00:00:00Z", "90"),
                row(labA, 9, "2026-10-09T00:00:00Z", "80"),
                row(labA, 8, "2026-10-08T00:00:00Z", "70"),
                row(labA, 7, "2026-10-07T00:00:00Z", "60"),
                row(labA, 6, "2026-10-06T00:00:00Z", "50"),
                row(labA, 5, "2026-10-05T00:00:00Z", "45"),
                row(labA, 4, "2026-10-04T00:00:00Z", "44"),
                row(labA, 3, "2026-10-03T00:00:00Z", "43"),
                row(labA, 2, "2026-10-02T00:00:00Z", "42"),
                row(labA, 1, "2026-10-01T00:00:00Z", "10"));

        List<UUID> deleted = SubmissionRetentionKeepers.idsToPrune(rows, 10);

        assertEquals(List.of(rows.get(10).getId()), deleted);
    }

    @Test
    void twelveOnOneLab_oldestIsHighest_keepsOldestPlusNewestTen() {
        LabSubmission oldestBest = row(labA, 1, "2026-10-01T00:00:00Z", "100");
        List<LabSubmission> rows = List.of(
                row(labA, 12, "2026-10-12T00:00:00Z", "10"),
                row(labA, 11, "2026-10-11T00:00:00Z", "11"),
                row(labA, 10, "2026-10-10T00:00:00Z", "12"),
                row(labA, 9, "2026-10-09T00:00:00Z", "13"),
                row(labA, 8, "2026-10-08T00:00:00Z", "14"),
                row(labA, 7, "2026-10-07T00:00:00Z", "15"),
                row(labA, 6, "2026-10-06T00:00:00Z", "16"),
                row(labA, 5, "2026-10-05T00:00:00Z", "17"),
                row(labA, 4, "2026-10-04T00:00:00Z", "18"),
                row(labA, 3, "2026-10-03T00:00:00Z", "19"),
                row(labA, 2, "2026-10-02T00:00:00Z", "20"),
                oldestBest);

        List<UUID> deleted = SubmissionRetentionKeepers.idsToPrune(rows, 10);

        assertEquals(1, deleted.size());
        assertEquals(rows.get(10).getId(), deleted.get(0));
        assertTrue(deleted.stream().noneMatch(id -> id.equals(oldestBest.getId())));
    }

    @Test
    void recentSpamOnOneLab_stillKeepsOtherLabHighest() {
        LabSubmission labBBest = row(labB, 1, "2026-09-01T00:00:00Z", "99");
        LabSubmission labBOldLow = row(labB, 2, "2026-09-02T00:00:00Z", "5");
        List<LabSubmission> rows = List.of(
                row(labA, 10, "2026-10-10T00:00:00Z", "1"),
                row(labA, 9, "2026-10-09T00:00:00Z", "1"),
                row(labA, 8, "2026-10-08T00:00:00Z", "1"),
                row(labA, 7, "2026-10-07T00:00:00Z", "1"),
                row(labA, 6, "2026-10-06T00:00:00Z", "1"),
                row(labA, 5, "2026-10-05T00:00:00Z", "1"),
                row(labA, 4, "2026-10-04T00:00:00Z", "1"),
                row(labA, 3, "2026-10-03T00:00:00Z", "1"),
                row(labA, 2, "2026-10-02T00:00:00Z", "1"),
                row(labA, 1, "2026-10-01T00:00:00Z", "1"),
                labBOldLow,
                labBBest);

        Set<UUID> deleted = Set.copyOf(SubmissionRetentionKeepers.idsToPrune(rows, 10));

        assertEquals(Set.of(labBOldLow.getId()), deleted);
        assertTrue(deleted.stream().noneMatch(id -> id.equals(labBBest.getId())));
    }

    @Test
    void emptyOrDisabled_noDeletes() {
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of(), 10).isEmpty());
        LabSubmission only = row(labA, 1, "2026-10-01T00:00:00Z", "50");
        assertTrue(SubmissionRetentionKeepers.idsToPrune(List.of(only), 10).isEmpty());
    }

    @Test
    void tenOrFewer_noDeletes() {
        List<LabSubmission> rows = List.of(
                row(labA, 2, "2026-10-02T00:00:00Z", "10"),
                row(labB, 1, "2026-10-01T00:00:00Z", "90"));
        assertEquals(List.of(), SubmissionRetentionKeepers.idsToPrune(rows, 10));
    }

    private static LabSubmission row(UUID labId, int attempt, String submittedAt, String score) {
        Lab lab = new Lab();
        lab.setId(labId);
        LabSubmission submission = new LabSubmission();
        submission.setId(UUID.randomUUID());
        submission.setLab(lab);
        submission.setAttemptNumber(attempt);
        submission.setSubmittedAt(OffsetDateTime.parse(submittedAt).withOffsetSameInstant(ZoneOffset.UTC));
        submission.setScore(new BigDecimal(score));
        return submission;
    }
}
