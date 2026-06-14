package br.com.controledespesas.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class AppConfig {

    private static final String DB_HOST = "DB_HOST";
    private static final String DB_PORT = "DB_PORT";
    private static final String DB_NAME = "DB_NAME";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";

    private static final Dotenv DOTENV = Dotenv.configure()
            .directory(".")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    private final Function<String, String> valueProvider;

    public AppConfig() {
        this(AppConfig::readValue);
    }

    AppConfig(Function<String, String> valueProvider) {
        this.valueProvider = Objects.requireNonNull(valueProvider, "Value provider must not be null.");
    }

    public static AppConfig load() {
        return new AppConfig();
    }

    static AppConfig fromMap(Map<String, String> values) {
        return new AppConfig(values::get);
    }

    public String getDatabaseHost() {
        return requireValue(DB_HOST);
    }

    public int getDatabasePort() {
        String portValue = requireValue(DB_PORT);

        try {
            return Integer.parseInt(portValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Environment variable DB_PORT must be a valid integer.");
        }
    }

    public String getDatabaseName() {
        return requireValue(DB_NAME);
    }

    public String getDatabaseUser() {
        return requireValue(DB_USER);
    }

    public String getDatabasePassword() {
        return requireValue(DB_PASSWORD);
    }

    public String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true",
                getDatabaseHost(),
                getDatabasePort(),
                getDatabaseName()
        );
    }

    private String requireValue(String key) {
        String value = valueProvider.apply(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable " + key + " is missing.");
        }
        return value;
    }

    private static String readValue(String key) {
        String systemValue = System.getenv(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        return DOTENV.get(key);
    }
}
