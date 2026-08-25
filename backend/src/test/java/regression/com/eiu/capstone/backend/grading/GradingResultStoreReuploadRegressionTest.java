package regression.com.eiu.capstone.backend.grading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.GradingResultStore;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;

@ExtendWith(MockitoExtension.class)
class GradingResultStoreReuploadRegressionTest {

    @Mock private SubmissionFieldResultRepository fieldResults;
    @Mock private SubmissionMethodResultRepository methodResults;
    @Mock private SubmissionConstructorResultRepository constructorResults;
    @Mock private SubmissionRelationResultRepository relationResults;
    @Mock private SubmissionChallengeResultRepository challengeResults;
    @Mock private SubmissionTestcaseResultRepository testcaseResults;

    private GradingResultStore store;

    @BeforeEach
    void createStore() {
        store = new GradingResultStore(
                fieldResults, methodResults, constructorResults, relationResults, challengeResults, testcaseResults);
    }

    @Test
    void secondSaveDoesNotDeleteExistingRows() {
        GradingService.GradingComputationResult computed = emptyComputed();
        store.save(computed);
        store.save(computed);
        verifySavedTwice();
        verifyNeverDeleted();
    }

    @Test
    void emptyComputedListsDoNotThrow() {
        store.save(emptyComputed());
    }

    private void verifySavedTwice() {
        verify(fieldResults, times(2)).saveAll(any());
        verify(methodResults, times(2)).saveAll(any());
        verify(constructorResults, times(2)).saveAll(any());
        verify(relationResults, times(2)).saveAll(any());
        verify(challengeResults, times(2)).saveAll(any());
        verify(testcaseResults, times(2)).saveAll(any());
    }

    private void verifyNeverDeleted() {
        verify(fieldResults, never()).deleteAll();
        verify(methodResults, never()).deleteAll();
        verify(constructorResults, never()).deleteAll();
        verify(relationResults, never()).deleteAll();
        verify(challengeResults, never()).deleteAll();
        verify(testcaseResults, never()).deleteAll();
    }

    private static GradingService.GradingComputationResult emptyComputed() {
        GradingService.GradingComputationResult computed = new GradingService.GradingComputationResult();
        computed.fieldResults = List.of();
        computed.methodResults = List.of();
        computed.constructorResults = List.of();
        computed.relationResults = List.of();
        computed.challengeResults = List.of();
        computed.testcaseResults = List.of();
        return computed;
    }
}
