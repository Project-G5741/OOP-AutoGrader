package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload;
import com.eiu.capstone.backend.grading.H2GradingResultJdbcWriter;

class H2GradingResultJdbcWriterTest {

    private JdbcDataSource dataSource;
    private UUID submissionId;
    private UUID fieldId;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:h2writer;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS submission_method_result");
            statement.execute("DROP TABLE IF EXISTS submission_field_result");
            // Column order differs from JPA field order — MERGE must name columns explicitly.
            statement.execute("""
                    CREATE TABLE submission_field_result (
                        field_id UUID NOT NULL,
                        id UUID PRIMARY KEY,
                        is_correct BOOLEAN NOT NULL,
                        submission_id UUID NOT NULL,
                        CONSTRAINT submission_field_result_key UNIQUE (submission_id, field_id)
                    )
                    """);
        }
        submissionId = UUID.randomUUID();
        fieldId = UUID.randomUUID();
    }

    @Test
    void mergeFlagsStoresBooleanCorrectColumn() throws Exception {
        H2GradingResultJdbcWriter writer = new H2GradingResultJdbcWriter(dataSource);
        writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new GradingDetailPersistPayload.MemberFlag(fieldId, true)),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT is_correct FROM submission_field_result
                     WHERE submission_id = ? AND field_id = ?
                     """)) {
            statement.setObject(1, submissionId);
            statement.setObject(2, fieldId);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(true, rs.getBoolean(1));
            }
        }
    }

    @Test
    void upsertDetailsBorrowsConnectionOnceForMultiTablePayload() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE submission_method_result (
                        method_id UUID NOT NULL,
                        id UUID PRIMARY KEY,
                        is_correct BOOLEAN NOT NULL,
                        submission_id UUID NOT NULL,
                        CONSTRAINT submission_method_result_key UNIQUE (submission_id, method_id)
                    )
                    """);
        }
        AtomicInteger acquires = new AtomicInteger();
        DataSource counting = countingDataSource(dataSource, acquires);
        H2GradingResultJdbcWriter writer = new H2GradingResultJdbcWriter(counting);
        writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new GradingDetailPersistPayload.MemberFlag(fieldId, true)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), false)),
                List.of(),
                List.of(),
                List.of()));
        assertEquals(1, acquires.get());
    }

    @Test
    void midBatchFailureRollsBackEarlierDetailWrites() throws Exception {
        // Only field table exists — method MERGE fails after field MERGE in the same TX.
        H2GradingResultJdbcWriter writer = new H2GradingResultJdbcWriter(dataSource);
        assertThrows(IllegalStateException.class, () -> writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new GradingDetailPersistPayload.MemberFlag(fieldId, true)),
                List.of(new GradingDetailPersistPayload.MemberFlag(UUID.randomUUID(), false)),
                List.of(),
                List.of(),
                List.of())));

        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM submission_field_result WHERE submission_id = ?")) {
            statement.setObject(1, submissionId);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    private static DataSource countingDataSource(DataSource delegate, AtomicInteger acquires) {
        return new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                acquires.incrementAndGet();
                return delegate.getConnection();
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                acquires.incrementAndGet();
                return delegate.getConnection(username, password);
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return delegate.getLogWriter();
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                delegate.setLogWriter(out);
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                delegate.setLoginTimeout(seconds);
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return delegate.getLoginTimeout();
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return delegate.getParentLogger();
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return delegate.unwrap(iface);
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return delegate.isWrapperFor(iface);
            }
        };
    }
}
