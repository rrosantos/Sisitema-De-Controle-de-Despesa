package br.com.controledespesas.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
/**
 * Define uma fabrica funcional para obtencao de conexoes JDBC.
 */
public interface ConnectionProvider {

    Connection getConnection() throws SQLException;
}
