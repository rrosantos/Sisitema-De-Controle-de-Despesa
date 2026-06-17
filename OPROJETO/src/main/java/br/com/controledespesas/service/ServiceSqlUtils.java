package br.com.controledespesas.service;

import java.sql.Connection;
import java.sql.SQLException;

final class ServiceSqlUtils {

    private static final int DUPLICATE_KEY_ERROR_CODE = 1062;
    private static final int FOREIGN_KEY_RESTRICT_ERROR_CODE = 1451;

    private ServiceSqlUtils() {
    }

    static boolean isDuplicateKey(SQLException exception) {
        return exception != null && exception.getErrorCode() == DUPLICATE_KEY_ERROR_CODE;
    }

    static boolean isForeignKeyRestriction(SQLException exception) {
        return exception != null && exception.getErrorCode() == FOREIGN_KEY_RESTRICT_ERROR_CODE;
    }

    static void rollback(Connection connection, Throwable cause) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            if (cause != null) {
                cause.addSuppressed(rollbackException);
            }
        }
    }

    static void restoreAutoCommit(Connection connection, boolean originalAutoCommit, Throwable cause) throws SQLException {
        if (connection == null) {
            return;
        }

        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException exception) {
            if (cause != null) {
                cause.addSuppressed(exception);
                return;
            }
            throw exception;
        }
    }
}
