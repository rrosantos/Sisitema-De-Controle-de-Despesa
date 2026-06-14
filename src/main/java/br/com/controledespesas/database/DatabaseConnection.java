package br.com.controledespesas.database;

import br.com.controledespesas.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        AppConfig config = AppConfig.load();
        return DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getDatabaseUser(),
                config.getDatabasePassword()
        );
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (SQLException | IllegalStateException exception) {
            System.err.println("Database connection test failed: " + exception.getMessage());
            return false;
        }
    }
}
