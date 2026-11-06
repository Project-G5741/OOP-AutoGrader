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

    /**
     * One DataSource borrow with an explicit transaction: disable autocommit, run work,
     * commit on success, rollback on {@link SQLException} or {@link RuntimeException}, then restore
     * autocommit and release. Used by detail UPSERT so all member/testcase/assertion writes share
     * one checkout cycle.
     */
    static void withTransaction(DataSource dataSource, String failureMessage, ConnectionWork work) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException e) {
                rollbackQuietly(connection, e);
                throw new IllegalStateException(failureMessage, e);
            } catch (RuntimeException e) {
                rollbackQuietly(connection, e);
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Connection may already be broken; release still runs.
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(failureMessage, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private static void rollbackQuietly(Connection connection, Exception primary) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            primary.addSuppressed(rollbackEx);
        }
    }

    static String statusName(TestcaseResultStatus status) {
        return status == null ? TestcaseResultStatus.ERROR.name() : status.name();
    }

    static boolean isUniqueViolation(IllegalStateException e) {
        return e.getCause() instanceof SQLException sql && "23505".equals(sql.getSQLState());
    }
}
