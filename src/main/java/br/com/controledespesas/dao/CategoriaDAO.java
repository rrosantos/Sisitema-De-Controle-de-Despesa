package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CategoriaDAO {

    public Long inserir(Categoria categoria) throws SQLException {
        Objects.requireNonNull(categoria, "Category must not be null.");
        Objects.requireNonNull(categoria.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(categoria.getTipo(), "Category type must not be null.");

        String sql = """
                INSERT INTO categorias (usuario_id, nome, tipo, descricao, ativo)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, categoria.getUsuarioId());
            statement.setString(2, normalizeRequiredText(categoria.getNome(), "Category name"));
            statement.setString(3, categoria.getTipo().getValorBanco());
            statement.setString(4, normalizeOptionalText(categoria.getDescricao()));
            statement.setBoolean(5, categoria.isAtivo());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting category failed because no rows were affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    categoria.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting category failed because no generated key was returned.");
        }
    }

    public Optional<Categoria> buscarPorId(Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(id, "Category ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, descricao, ativo, criado_em, atualizado_em
                FROM categorias
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearCategoria(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Categoria> listarPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, descricao, ativo, criado_em, atualizado_em
                FROM categorias
                WHERE usuario_id = ?
                ORDER BY nome
                """;

        return executarListagem(sql, usuarioId);
    }

    public List<Categoria> listarAtivasPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, descricao, ativo, criado_em, atualizado_em
                FROM categorias
                WHERE usuario_id = ?
                  AND ativo = TRUE
                ORDER BY nome
                """;

        return executarListagem(sql, usuarioId);
    }

    public List<Categoria> listarPorUsuarioETipo(Long usuarioId, TipoCategoria tipo) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(tipo, "Category type must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, descricao, ativo, criado_em, atualizado_em
                FROM categorias
                WHERE usuario_id = ?
                  AND tipo = ?
                ORDER BY nome
                """;

        List<Categoria> categorias = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setString(2, tipo.getValorBanco());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(mapearCategoria(resultSet));
                }
            }
        }

        return categorias;
    }

    public boolean nomeETipoExistem(Long usuarioId, String nome, TipoCategoria tipo) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(tipo, "Category type must not be null.");

        String sql = """
                SELECT 1
                FROM categorias
                WHERE usuario_id = ?
                  AND nome = ?
                  AND tipo = ?
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setString(2, normalizeRequiredText(nome, "Category name"));
            statement.setString(3, tipo.getValorBanco());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean atualizar(Categoria categoria) throws SQLException {
        Objects.requireNonNull(categoria, "Category must not be null.");
        Objects.requireNonNull(categoria.getId(), "Category ID must not be null.");
        Objects.requireNonNull(categoria.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(categoria.getTipo(), "Category type must not be null.");

        String sql = """
                UPDATE categorias
                SET nome = ?, tipo = ?, descricao = ?, ativo = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeRequiredText(categoria.getNome(), "Category name"));
            statement.setString(2, categoria.getTipo().getValorBanco());
            statement.setString(3, normalizeOptionalText(categoria.getDescricao()));
            statement.setBoolean(4, categoria.isAtivo());
            statement.setLong(5, categoria.getId());
            statement.setLong(6, categoria.getUsuarioId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean atualizarStatus(Long categoriaId, Long usuarioId, boolean ativo) throws SQLException {
        Objects.requireNonNull(categoriaId, "Category ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                UPDATE categorias
                SET ativo = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, ativo);
            statement.setLong(2, categoriaId);
            statement.setLong(3, usuarioId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean excluir(Long categoriaId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(categoriaId, "Category ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                DELETE FROM categorias
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoriaId);
            statement.setLong(2, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    private List<Categoria> executarListagem(String sql, Long usuarioId) throws SQLException {
        List<Categoria> categorias = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(mapearCategoria(resultSet));
                }
            }
        }

        return categorias;
    }

    private Categoria mapearCategoria(ResultSet resultSet) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(resultSet.getLong("id"));
        categoria.setUsuarioId(resultSet.getLong("usuario_id"));
        categoria.setNome(resultSet.getString("nome"));
        categoria.setTipo(TipoCategoria.fromValorBanco(resultSet.getString("tipo")));
        categoria.setDescricao(resultSet.getString("descricao"));
        categoria.setAtivo(resultSet.getBoolean("ativo"));
        categoria.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        categoria.setAtualizadoEm(toLocalDateTime(resultSet.getTimestamp("atualizado_em")));
        return categoria;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null.");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
