package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.service.SubmissionRetentionService;
import com.eiu.capstone.backend.service.SubmissionRetentionTx;

class SubmissionRetentionLateFlagTest {

    @Test
    void willPruneLateUpload_onlyTrueWhenEnabledAndSubmittedAfterDeadline() {
        SubmissionRetentionService enabledService = new SubmissionRetentionService(
                true,
                1,
                200,
                mock(LabSubmissionRepository.class),
                mock(SubmissionRetentionTx.class));
        SubmissionRetentionService disabledService = new SubmissionRetentionService(
                false,
                1,
                200,
                mock(LabSubmissionRepository.class),
                mock(SubmissionRetentionTx.class));

        OffsetDateTime submittedNov12 = OffsetDateTime.of(2026, 11, 12, 13, 52, 0, 0, ZoneOffset.ofHours(7));

        Lab expiredLab = new Lab();
        expiredLab.setDeadlineDate(LocalDate.of(2026, 11, 10));
        LabSubmission lateSubmission = new LabSubmission();
        lateSubmission.setLab(expiredLab);
        lateSubmission.setSubmittedAt(submittedNov12);

        Lab openLab = new Lab();
        openLab.setDeadlineDate(LocalDate.of(2026, 11, 14));
        LabSubmission onTimeSubmission = new LabSubmission();
        onTimeSubmission.setLab(openLab);
        onTimeSubmission.setSubmittedAt(submittedNov12);

        Lab noDeadlineLab = new Lab();
        LabSubmission undatedSubmission = new LabSubmission();
        undatedSubmission.setLab(noDeadlineLab);
        undatedSubmission.setSubmittedAt(submittedNov12);

        assertTrue(enabledService.willPruneLateUpload(lateSubmission));
        assertFalse(enabledService.willPruneLateUpload(onTimeSubmission));
        assertFalse(enabledService.willPruneLateUpload(undatedSubmission));
        assertFalse(enabledService.willPruneLateUpload(null));
        assertFalse(disabledService.willPruneLateUpload(lateSubmission));
    }
}
