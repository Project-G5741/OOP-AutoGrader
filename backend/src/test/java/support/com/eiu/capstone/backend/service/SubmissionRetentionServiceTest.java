package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.service.SubmissionRetentionService;
import com.eiu.capstone.backend.service.SubmissionRetentionTx;

@ExtendWith(MockitoExtension.class)
class SubmissionRetentionServiceTest {

    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    @Mock
    private SubmissionRetentionTx submissionRetentionTx;

    private SubmissionRetentionService retentionService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        retentionService = new SubmissionRetentionService(
                true,
                10,
                200,
                labSubmissionRepository,
                submissionRetentionTx);
    }

    @Test
    void pruneAllExistingOverCap_loopsUntilNoStudents() {
        when(labSubmissionRepository.findStudentsOverRetentionCap(10, 200))
                .thenReturn(List.of(userId), Collections.emptyList());
        when(submissionRetentionTx.enforceForStudent(userId)).thenReturn(5);

        int students = retentionService.pruneAllExistingOverCap();

        assertEquals(1, students);
        verify(labSubmissionRepository, times(2)).findStudentsOverRetentionCap(10, 200);
        verify(submissionRetentionTx).enforceForStudent(userId);
    }

    @Test
    void enforceForStudent_whenDisabled_doesNothing() {
        SubmissionRetentionService disabled = new SubmissionRetentionService(
                false,
                10,
                200,
                labSubmissionRepository,
                submissionRetentionTx);

        assertEquals(0, disabled.enforceForStudent(userId));
        verify(submissionRetentionTx, never()).enforceForStudent(eq(userId));
    }

    @Test
    void enforceForStudent_delegatesToTx() {
        when(submissionRetentionTx.enforceForStudent(userId)).thenReturn(2);

        assertEquals(2, retentionService.enforceForStudent(userId));
    }
}
