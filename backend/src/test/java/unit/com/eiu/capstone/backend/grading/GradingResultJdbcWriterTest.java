package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload;
import com.eiu.capstone.backend.grading.GradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.PostgresGradingResultJdbcWriter;
import com.eiu.capstone.backend.grading.GradingService;

@ExtendWith(MockitoExtension.class)
class GradingResultJdbcWriterTest {

    @Mock private DataSource dataSource;

    @Test
    void emptyListsDoNotOpenAConnection() throws Exception {
        GradingResultJdbcWriter writer = new PostgresGradingResultJdbcWriter(dataSource);
        assertDoesNotThrow(() -> writer.upsertChallengeResults(List.of()));
        GradingService.GradingComputationResult computed = new GradingService.GradingComputationResult();
        computed.fieldResults = List.of();
        computed.methodResults = List.of();
        computed.constructorResults = List.of();
        computed.relationResults = List.of();
        computed.challengeResults = List.of();
        computed.testcaseResults = List.of();
        GradingDetailPersistPayload fromComputed = GradingDetailPersistPayload.from(computed);
        assertDoesNotThrow(() -> writer.upsertDetails(fromComputed));

        UUID submissionId = UUID.randomUUID();
        GradingDetailPersistPayload emptyWithId = new GradingDetailPersistPayload(
                submissionId, List.of(), List.of(), List.of(), List.of(), List.of());
        assertDoesNotThrow(() -> writer.upsertDetails(emptyWithId));
        verify(dataSource, never()).getConnection();
    }

    @Test
    void multiTableUpsertDetailsBorrowsConnectionOnce() throws Exception {
        AtomicInteger acquires = new AtomicInteger();
        DataSource counting = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        Array array = mock(Array.class);
        when(counting.getConnection()).thenAnswer(invocation -> {
            acquires.incrementAndGet();
            return connection;
        });
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(connection.createArrayOf(anyString(), any())).thenReturn(array);

        PostgresGradingResultJdbcWriter writer = new PostgresGradingResultJdbcWriter(counting);
        UUID submissionId = UUID.randomUUID();
        writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), true)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), false)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), true)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), true)),
                List.of()));

        assertEquals(1, acquires.get());
        verify(connection, times(1)).commit();
        verify(connection, never()).rollback();
        verify(connection).setAutoCommit(false);
        verify(connection).setAutoCommit(true);
    }

    @Test
    void midBatchFailureRollsBackDetailTransaction() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ok = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createArrayOf(anyString(), any())).thenReturn(mock(Array.class));
        when(connection.prepareStatement(anyString()))
                .thenReturn(ok)
                .thenThrow(new SQLException("forced second-table failure"));

        PostgresGradingResultJdbcWriter writer = new PostgresGradingResultJdbcWriter(ds);
        UUID submissionId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), true)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), false)),
                List.of(),
                List.of(),
                List.of())));

        verify(connection).rollback();
        verify(connection, never()).commit();
    }
}
