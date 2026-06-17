package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * The DAO expects senhaHash to already be prepared for storage.
 * Password hashing is intentionally not performed in this layer.
 */
public class UsuarioDAO {

    public Long inserir(Usuario usuario) throws SQLException {
        Objects.requireNonNull(usuario, "User must not be null.");

        String sql = """
                INSERT INTO usuarios (nome, email, senha, ativo)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, normalizeRequiredText(usuario.getNome(), "User name"));
            statement.setString(2, normalizeEmail(usuario.getEmail()));
            statement.setString(3, normalizeRequiredText(usuario.getSenhaHash(), "Password hash"));
            statement.setBoolean(4, usuario.isAtivo());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting user failed because no rows were affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    usuario.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting user failed because no generated key was returned.");
        }
    }

    public Optional<Usuario> buscarPorId(Long id) throws SQLException {
        Objects.requireNonNull(id, "User ID must not be null.");

        String sql = """
                SELECT id, nome, email, senha, ativo, criado_em, atualizado_em
                FROM usuarios
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearUsuario(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Usuario> buscarPorEmail(String email) throws SQLException {
        String sql = """
                SELECT id, nome, email, senha, ativo, criado_em, atualizado_em
                FROM usuarios
                WHERE email = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearUsuario(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public boolean emailExiste(String email) throws SQLException {
        String sql = """
                SELECT 1
                FROM usuarios
                WHERE email = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean emailExisteParaOutroUsuario(String email, Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT 1
                FROM usuarios
                WHERE email = ?
                  AND id <> ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean atualizar(Usuario usuario) throws SQLException {
        Objects.requireNonNull(usuario, "User must not be null.");
        Objects.requireNonNull(usuario.getId(), "User ID must not be null.");

        String sql = """
                UPDATE usuarios
                SET nome = ?, email = ?, ativo = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeRequiredText(usuario.getNome(), "User name"));
            statement.setString(2, normalizeEmail(usuario.getEmail()));
            statement.setBoolean(3, usuario.isAtivo());
            statement.setLong(4, usuario.getId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean atualizarSenha(Long usuarioId, String senhaHash) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                UPDATE usuarios
                SET senha = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeRequiredText(senhaHash, "Password hash"));
            statement.setLong(2, usuarioId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean atualizarStatus(Long usuarioId, boolean ativo) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                UPDATE usuarios
                SET ativo = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, ativo);
            statement.setLong(2, usuarioId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean excluir(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    private Usuario mapearUsuario(ResultSet resultSet) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setId(resultSet.getLong("id"));
        usuario.setNome(resultSet.getString("nome"));
        usuario.setEmail(resultSet.getString("email"));
        usuario.setSenhaHash(resultSet.getString("senha"));
        usuario.setAtivo(resultSet.getBoolean("ativo"));
        usuario.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        usuario.setAtualizadoEm(toLocalDateTime(resultSet.getTimestamp("atualizado_em")));
        return usuario;
    }

    private String normalizeEmail(String email) {
        return normalizeRequiredText(email, "Email");
    }

    private String normalizeRequiredText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null.");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
