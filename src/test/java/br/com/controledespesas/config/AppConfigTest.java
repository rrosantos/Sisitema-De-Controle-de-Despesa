package br.com.controledespesas.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void shouldBuildJdbcUrlFromProvidedValues() {
        AppConfig config = AppConfig.fromMap(Map.of(
                "DB_HOST", "localhost",
                "DB_PORT", "3306",
                "DB_NAME", "controle_despesas",
                "DB_USER", "controle_app",
                "DB_PASSWORD", "troque_esta_senha"
        ));

        String jdbcUrl = config.getJdbcUrl();

        assertEquals(
                "jdbc:mysql://localhost:3306/controle_despesas?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true",
                jdbcUrl
        );
    }
}
