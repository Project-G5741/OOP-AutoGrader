package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.exception.DailySubmissionQuotaExceededException;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.service.StudentDailySubmissionQuotaService;

@ExtendWith(MockitoExtension.class)
class StudentDailySubmissionQuotaServiceTest {

    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    private StudentDailySubmissionQuotaService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentDailySubmissionQuotaService(labSubmissionRepository, 30);
    }

    @Test
    void viewFor_remainingAndCanSubmit() {
        when(labSubmissionRepository.countByUser_IdAndSubmittedAtBetween(
                eq(userId), any(), any()))
                .thenReturn(12L);

        var view = service.viewFor(userId);

        assertEquals(12, view.usedToday());
        assertEquals(30, view.dailyLimit());
        assertEquals(18, view.remaining());
        assertTrue(view.canSubmit());
    }

    @Test
    void requireCanSubmit_throwsWhenAtCap() {
        when(labSubmissionRepository.countByUser_IdAndSubmittedAtBetween(
                eq(userId), any(), any()))
                .thenReturn(30L);

        assertThrows(DailySubmissionQuotaExceededException.class, () -> service.requireCanSubmit(userId));

        var view = service.viewFor(userId);
        assertEquals(30, view.usedToday());
        assertFalse(view.canSubmit());
    }
}
