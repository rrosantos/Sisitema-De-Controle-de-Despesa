package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.StatusCofrinho;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CofrinhoDAO {

    public Long inserir(Cofrinho cofrinho) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return inserir(connection, cofrinho);
        }
    }

    public Long inserir(Connection connection, Cofrinho cofrinho) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(cofrinho, "Savings goal must not be null.");
        Objects.requireNonNull(cofrinho.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(cofrinho.getValorMeta(), "Target value must not be null.");
        Objects.requireNonNull(cofrinho.getStatus(), "Savings goal status must not be null.");

        String sql = """
                INSERT INTO cofrinhos (usuario_id, nome, descricao, valor_meta, data_limite, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, cofrinho.getUsuarioId());
            statement.setString(2, normalizeRequiredText(cofrinho.getNome(), "Savings goal name"));
            statement.setString(3, normalizeOptionalText(cofrinho.getDescricao()));
            statement.setBigDecimal(4, cofrinho.getValorMeta());
            setNullableDate(statement, 5, cofrinho.getDataLimite());
            statement.setString(6, cofrinho.getStatus().getValorBanco());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting savings goal failed because no rows were affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    cofrinho.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting savings goal failed because no generated key was returned.");
        }
    }

    public Optional<Cofrinho> buscarPorId(Long id, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return buscarPorId(connection, id, usuarioId);
        }
    }

    public Optional<Cofrinho> buscarPorId(Connection connection, Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(id, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, descricao, valor_meta, data_limite, status, criado_em, atualizado_em
                FROM cofrinhos
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearCofrinho(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<Cofrinho> buscarPorIdParaAtualizacao(Connection connection, Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(id, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, descricao, valor_meta, data_limite, status, criado_em, atualizado_em
                FROM cofrinhos
                WHERE id = ?
                  AND usuario_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearCofrinho(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Cofrinho> listarPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, descricao, valor_meta, data_limite, status, criado_em, atualizado_em
                FROM cofrinhos
                WHERE usuario_id = ?
                ORDER BY
                    CASE status
                        WHEN 'em_andamento' THEN 0
                        WHEN 'concluido' THEN 1
                        ELSE 2
                    END,
                    data_limite IS NULL,
                    data_limite,
                    nome
                """;

        return executarListagem(sql, usuarioId, null);
    }

    public List<Cofrinho> listarPorUsuarioEStatus(Long usuarioId, StatusCofrinho status) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(status, "Savings goal status must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, descricao, valor_meta, data_limite, status, criado_em, atualizado_em
                FROM cofrinhos
                WHERE usuario_id = ?
                  AND status = ?
                ORDER BY data_limite IS NULL, data_limite, nome
                """;

        return executarListagem(sql, usuarioId, status);
    }

    public boolean atualizar(Cofrinho cofrinho) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return atualizar(connection, cofrinho);
        }
    }

    public boolean atualizar(Connection connection, Cofrinho cofrinho) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(cofrinho, "Savings goal must not be null.");
        Objects.requireNonNull(cofrinho.getId(), "Savings goal ID must not be null.");
        Objects.requireNonNull(cofrinho.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(cofrinho.getValorMeta(), "Target value must not be null.");
        Objects.requireNonNull(cofrinho.getStatus(), "Savings goal status must not be null.");

        String sql = """
                UPDATE cofrinhos
                SET nome = ?, descricao = ?, valor_meta = ?, data_limite = ?, status = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeRequiredText(cofrinho.getNome(), "Savings goal name"));
            statement.setString(2, normalizeOptionalText(cofrinho.getDescricao()));
            statement.setBigDecimal(3, cofrinho.getValorMeta());
            setNullableDate(statement, 4, cofrinho.getDataLimite());
            statement.setString(5, cofrinho.getStatus().getValorBanco());
            statement.setLong(6, cofrinho.getId());
            statement.setLong(7, cofrinho.getUsuarioId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean atualizarStatus(Long cofrinhoId, Long usuarioId, StatusCofrinho status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return atualizarStatus(connection, cofrinhoId, usuarioId, status);
        }
    }

    public boolean atualizarStatus(Connection connection, Long cofrinhoId, Long usuarioId, StatusCofrinho status)
            throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(cofrinhoId, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(status, "Savings goal status must not be null.");

        String sql = """
                UPDATE cofrinhos
                SET status = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.getValorBanco());
            statement.setLong(2, cofrinhoId);
            statement.setLong(3, usuarioId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean excluir(Long cofrinhoId, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return excluir(connection, cofrinhoId, usuarioId);
        }
    }

    public boolean excluir(Connection connection, Long cofrinhoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(cofrinhoId, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                DELETE FROM cofrinhos
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cofrinhoId);
            statement.setLong(2, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    private List<Cofrinho> executarListagem(String sql, Long usuarioId, StatusCofrinho status) throws SQLException {
        List<Cofrinho> cofrinhos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            if (status != null) {
                statement.setString(2, status.getValorBanco());
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    cofrinhos.add(mapearCofrinho(resultSet));
                }
            }
        }

        return cofrinhos;
    }

    private Cofrinho mapearCofrinho(ResultSet resultSet) throws SQLException {
        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setId(resultSet.getLong("id"));
        cofrinho.setUsuarioId(resultSet.getLong("usuario_id"));
        cofrinho.setNome(resultSet.getString("nome"));
        cofrinho.setDescricao(resultSet.getString("descricao"));
        cofrinho.setValorMeta(resultSet.getBigDecimal("valor_meta"));
        Date dataLimite = resultSet.getDate("data_limite");
        cofrinho.setDataLimite(dataLimite != null ? dataLimite.toLocalDate() : null);
        cofrinho.setStatus(StatusCofrinho.fromValorBanco(resultSet.getString("status")));
        cofrinho.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        cofrinho.setAtualizadoEm(toLocalDateTime(resultSet.getTimestamp("atualizado_em")));
        return cofrinho;
    }

    private void setNullableDate(PreparedStatement statement, int parameterIndex, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.DATE);
        } else {
            statement.setDate(parameterIndex, Date.valueOf(value));
        }
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
