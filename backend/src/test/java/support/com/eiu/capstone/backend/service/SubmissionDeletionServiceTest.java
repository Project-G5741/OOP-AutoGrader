package support.com.eiu.capstone.backend.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;
import com.eiu.capstone.backend.service.SubmissionDeletionService;

@ExtendWith(MockitoExtension.class)
class SubmissionDeletionServiceTest {

    @Mock
    private SubmissionPlagiarismMatchRepository plagiarismMatchRepository;
    @Mock
    private SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository;
    @Mock
    private SubmissionTestcaseResultRepository submissionTestcaseResultRepository;
    @Mock
    private SubmissionFieldResultRepository submissionFieldResultRepository;
    @Mock
    private SubmissionMethodResultRepository submissionMethodResultRepository;
    @Mock
    private SubmissionConstructorResultRepository submissionConstructorResultRepository;
    @Mock
    private SubmissionChallengeResultRepository submissionChallengeResultRepository;
    @Mock
    private SubmissionRelationResultRepository submissionRelationResultRepository;
    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    private SubmissionDeletionService deletionService;

    @BeforeEach
    void setUp() {
        deletionService = new SubmissionDeletionService(
                plagiarismMatchRepository,
                plagiarismFingerprintRepository,
                submissionTestcaseResultRepository,
                submissionFieldResultRepository,
                submissionMethodResultRepository,
                submissionConstructorResultRepository,
                submissionChallengeResultRepository,
                submissionRelationResultRepository,
                labSubmissionRepository);
    }

    @Test
    void deleteSubmissionsFully_emptyList_noOp() {
        deletionService.deleteSubmissionsFully(List.of());
        verifyNoInteractions(plagiarismMatchRepository);
    }

    @Test
    void deleteSubmissionsFully_invokesRepositoriesInOrder() {
        UUID submissionId = UUID.randomUUID();
        deletionService.deleteSubmissionsFully(List.of(submissionId));

        verify(plagiarismMatchRepository).deleteInvolvingSubmission(submissionId);
        verify(plagiarismFingerprintRepository).deleteBySubmissionId(submissionId);
        verify(submissionTestcaseResultRepository).deleteAssertionResultsBySubmissionIds(List.of(submissionId));
        verify(submissionTestcaseResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionFieldResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionMethodResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionConstructorResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionRelationResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionChallengeResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(labSubmissionRepository).deleteAllByIdIn(List.of(submissionId));
    }

    @Test
    void stripDetailRows_keepsScoreRows() {
        UUID submissionId = UUID.randomUUID();
        deletionService.stripDetailRows(List.of(submissionId));

        verify(submissionTestcaseResultRepository).deleteAssertionResultsBySubmissionIds(List.of(submissionId));
        verify(submissionTestcaseResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionFieldResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionMethodResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionConstructorResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionRelationResultRepository).deleteBySubmissionIds(List.of(submissionId));
        verify(submissionChallengeResultRepository).clearMmdOutcomesBySubmissionIds(List.of(submissionId));
        verify(submissionChallengeResultRepository, never()).deleteBySubmissionIds(List.of(submissionId));
        verify(labSubmissionRepository, never()).deleteAllByIdIn(List.of(submissionId));
        verifyNoInteractions(plagiarismMatchRepository, plagiarismFingerprintRepository);
    }
}
