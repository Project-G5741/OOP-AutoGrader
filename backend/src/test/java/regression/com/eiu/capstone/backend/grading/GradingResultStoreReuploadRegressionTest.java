package regression.com.eiu.capstone.backend.grading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.GradingResultStore;
import com.eiu.capstone.backend.grading.GradingService;
import com.eiu.capstone.backend.grading.SubmissionDetailPersistGate;

@ExtendWith(MockitoExtension.class)
class GradingResultStoreReuploadRegressionTest {

    @Mock private GradingResultJdbcWriter jdbcWriter;
    @Mock private SubmissionDetailPersistGate persistGate;
    @Mock private ExecutorService persistExecutor;

    private GradingResultStore store;

    @BeforeEach
    void createStore() {
        store = new GradingResultStore(jdbcWriter, persistGate, persistExecutor);
    }

    @Test
    void secondSaveUpsertsWithoutHibernateSaveAll() {
        GradingService.GradingComputationResult computed = emptyComputed();
        store.save(computed);
        store.save(computed);
        verify(jdbcWriter, times(2)).upsertChallengeResults(any());
        verify(jdbcWriter, times(2)).upsertDetails(any());
    }

    @Test
    void emptyComputedListsDoNotThrow() {
        store.save(emptyComputed());
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
