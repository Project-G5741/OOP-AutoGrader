package unit.com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.GradingDetailPersistPayload;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.AssertionRow;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.MemberFlag;
import com.eiu.capstone.backend.grading.GradingDetailPersistPayload.TestcaseRow;
import com.eiu.capstone.backend.grading.PostgresGradingResultJdbcWriter;
import com.eiu.capstone.backend.model.TestcaseResultStatus;

class PostgresGradingResultJdbcWriterTest {

    @Test
    void upsertDetailsUsesColumnTargetsInsteadOfNamedConstraints() throws Exception {
        List<String> executedSql = new ArrayList<>();
        UUID submissionId = UUID.randomUUID();
        UUID testcaseId = UUID.randomUUID();
        UUID tcResultId = UUID.randomUUID();

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createArrayOf(anyString(), any())).thenReturn(mock(Array.class));
        when(connection.prepareStatement(anyString())).thenAnswer(inv -> {
            String sql = inv.getArgument(0);
            executedSql.add(sql);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            when(rs.getObject("testcase_id", UUID.class)).thenReturn(testcaseId);
            when(rs.getObject("id", UUID.class)).thenReturn(tcResultId);
            return ps;
        });

        PostgresGradingResultJdbcWriter writer = new PostgresGradingResultJdbcWriter(dataSource);
        writer.upsertDetails(new GradingDetailPersistPayload(
                submissionId,
                List.of(new MemberFlag(UUID.randomUUID(), true)),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TestcaseRow(
                        testcaseId,
                        TestcaseResultStatus.PASSED,
                        null,
                        "s1.run()",
                        "1",
                        "1",
                        List.of(new AssertionRow(
                                UUID.randomUUID(),
                                TestcaseResultStatus.PASSED,
                                "1",
                                null))))));

        assertFalse(executedSql.isEmpty());
        for (String sql : executedSql) {
            assertFalse(sql.contains("ON CONFLICT ON CONSTRAINT"), sql);
        }
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("ON CONFLICT (submission_id, field_id)")));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("ON CONFLICT (submission_id, testcase_id)")));
        assertTrue(executedSql.stream().anyMatch(
                sql -> sql.contains("ON CONFLICT (submission_testcase_result_id, testcase_assertion_id)")));
    }
}
