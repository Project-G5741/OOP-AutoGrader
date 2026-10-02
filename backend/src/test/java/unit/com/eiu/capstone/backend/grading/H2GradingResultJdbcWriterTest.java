package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

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
}
