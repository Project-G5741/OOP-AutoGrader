package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.GradingService;

import javax.sql.DataSource;

@ExtendWith(MockitoExtension.class)
class GradingResultJdbcWriterTest {

    @Mock private DataSource dataSource;

    @Test
    void emptyListsDoNotOpenAConnection() {
        GradingResultJdbcWriter writer = new GradingResultJdbcWriter(dataSource);
        assertDoesNotThrow(() -> writer.upsertChallengeResults(List.of()));
        GradingService.GradingComputationResult computed = new GradingService.GradingComputationResult();
        computed.fieldResults = List.of();
        computed.methodResults = List.of();
        computed.constructorResults = List.of();
        computed.relationResults = List.of();
        computed.challengeResults = List.of();
        computed.testcaseResults = List.of();
        GradingDetailPersistPayload payload = GradingDetailPersistPayload.from(computed);
        assertNull(payload.submissionId());
        assertDoesNotThrow(() -> writer.upsertDetails(payload));
    }
}
