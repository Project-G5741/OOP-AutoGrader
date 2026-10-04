package com.eiu.capstone.backend.grading;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.eiu.capstone.backend.model.TestcaseResultStatus;

final class GradingResultJdbcWriterSupport {

    private GradingResultJdbcWriterSupport() {}

    @FunctionalInterface
    interface ConnectionWork {
        void run(Connection connection) throws SQLException;
    }

    static void withConnection(DataSource dataSource, String failureMessage, ConnectionWork work) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            work.run(connection);
        } catch (SQLException e) {
            throw new IllegalStateException(failureMessage, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    static String statusName(TestcaseResultStatus status) {
        return status == null ? TestcaseResultStatus.ERROR.name() : status.name();
    }

    static boolean isUniqueViolation(IllegalStateException e) {
        return e.getCause() instanceof SQLException sql && "23505".equals(sql.getSQLState());
    }
}
