package br.com.controledespesas.database;

import br.com.controledespesas.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fornece conexoes JDBC configuradas e validacao basica de conectividade.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

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
            LOGGER.log(Level.WARNING, "Falha ao testar conexao com o banco de dados.", exception);
            return false;
        }
    }
}
