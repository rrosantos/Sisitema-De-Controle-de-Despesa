package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;
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

/**
 * Realiza a persistencia JDBC e consultas relacionadas a Conta.
 */
public class ContaDAO {

    public Long inserir(Conta conta) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return inserir(connection, conta);
        }
    }

    public Long inserir(Connection connection, Conta conta) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(conta, "Account must not be null.");
        Objects.requireNonNull(conta.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(conta.getTipo(), "Account type must not be null.");
        Objects.requireNonNull(conta.getSaldoInicial(), "Initial balance must not be null.");

        String sql = """
                INSERT INTO contas (usuario_id, nome, tipo, instituicao, saldo_inicial, ativo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, conta.getUsuarioId());
            statement.setString(2, normalizeRequiredText(conta.getNome(), "Account name"));
            statement.setString(3, conta.getTipo().getValorBanco());
            statement.setString(4, normalizeOptionalText(conta.getInstituicao()));
            statement.setBigDecimal(5, conta.getSaldoInicial());
            statement.setBoolean(6, conta.isAtivo());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting account failed because no rows were affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    conta.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting account failed because no generated key was returned.");
        }
    }

    public Optional<Conta> buscarPorId(Long id, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return buscarPorId(connection, id, usuarioId);
        }
    }

    public Optional<Conta> buscarPorId(Connection connection, Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(id, "Account ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, instituicao, saldo_inicial, ativo, criado_em, atualizado_em
                FROM contas
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearConta(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Conta> listarPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, instituicao, saldo_inicial, ativo, criado_em, atualizado_em
                FROM contas
                WHERE usuario_id = ?
                ORDER BY nome
                """;

        return executarListagem(sql, usuarioId);
    }

    public List<Conta> listarAtivasPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, nome, tipo, instituicao, saldo_inicial, ativo, criado_em, atualizado_em
                FROM contas
                WHERE usuario_id = ?
                  AND ativo = TRUE
                ORDER BY nome
                """;

        return executarListagem(sql, usuarioId);
    }

    public boolean nomeExiste(Long usuarioId, String nome) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return nomeExiste(connection, usuarioId, nome);
        }
    }

    public boolean nomeExiste(Connection connection, Long usuarioId, String nome) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT 1
                FROM contas
                WHERE usuario_id = ?
                  AND nome = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setString(2, normalizeRequiredText(nome, "Account name"));

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean nomeExisteParaOutraConta(Long usuarioId, String nome, Long contaId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return nomeExisteParaOutraConta(connection, usuarioId, nome, contaId);
        }
    }

    public boolean nomeExisteParaOutraConta(Connection connection, Long usuarioId, String nome, Long contaId)
            throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(contaId, "Account ID must not be null.");

        String sql = """
                SELECT 1
                FROM contas
                WHERE usuario_id = ?
                  AND nome = ?
                  AND id <> ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setString(2, normalizeRequiredText(nome, "Account name"));
            statement.setLong(3, contaId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean atualizar(Conta conta) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return atualizar(connection, conta);
        }
    }

    public boolean atualizar(Connection connection, Conta conta) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(conta, "Account must not be null.");
        Objects.requireNonNull(conta.getId(), "Account ID must not be null.");
        Objects.requireNonNull(conta.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(conta.getTipo(), "Account type must not be null.");
        Objects.requireNonNull(conta.getSaldoInicial(), "Initial balance must not be null.");

        String sql = """
                UPDATE contas
                SET nome = ?, tipo = ?, instituicao = ?, saldo_inicial = ?, ativo = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeRequiredText(conta.getNome(), "Account name"));
            statement.setString(2, conta.getTipo().getValorBanco());
            statement.setString(3, normalizeOptionalText(conta.getInstituicao()));
            statement.setBigDecimal(4, conta.getSaldoInicial());
            statement.setBoolean(5, conta.isAtivo());
            statement.setLong(6, conta.getId());
            statement.setLong(7, conta.getUsuarioId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean atualizarStatus(Long contaId, Long usuarioId, boolean ativo) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return atualizarStatus(connection, contaId, usuarioId, ativo);
        }
    }

    public boolean atualizarStatus(Connection connection, Long contaId, Long usuarioId, boolean ativo) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(contaId, "Account ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                UPDATE contas
                SET ativo = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, ativo);
            statement.setLong(2, contaId);
            statement.setLong(3, usuarioId);

            return statement.executeUpdate() > 0;
        }
    }

    public boolean excluir(Long contaId, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return excluir(connection, contaId, usuarioId);
        }
    }

    public boolean excluir(Connection connection, Long contaId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(contaId, "Account ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                DELETE FROM contas
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contaId);
            statement.setLong(2, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<BigDecimal> calcularSaldoAtual(Long contaId, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return calcularSaldoAtual(connection, contaId, usuarioId);
        }
    }

    public Optional<BigDecimal> calcularSaldoAtual(Connection connection, Long contaId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(contaId, "Account ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT c.saldo_inicial
                       + COALESCE(SUM(
                           CASE
                               WHEN t.tipo = 'receita' AND t.status = 'recebido' THEN t.valor
                               WHEN t.tipo = 'despesa' AND t.status = 'pago' THEN -t.valor
                               ELSE 0
                           END
                       ), 0) AS saldo_atual
                FROM contas c
                LEFT JOIN transacoes t
                    ON t.conta_id = c.id
                   AND t.usuario_id = c.usuario_id
                WHERE c.id = ?
                  AND c.usuario_id = ?
                GROUP BY c.id, c.saldo_inicial
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contaId);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getBigDecimal("saldo_atual"));
                }
            }
        }

        return Optional.empty();
    }

    private List<Conta> executarListagem(String sql, Long usuarioId) throws SQLException {
        List<Conta> contas = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contas.add(mapearConta(resultSet));
                }
            }
        }

        return contas;
    }

    private Conta mapearConta(ResultSet resultSet) throws SQLException {
        Conta conta = new Conta();
        conta.setId(resultSet.getLong("id"));
        conta.setUsuarioId(resultSet.getLong("usuario_id"));
        conta.setNome(resultSet.getString("nome"));
        conta.setTipo(TipoConta.fromValorBanco(resultSet.getString("tipo")));
        conta.setInstituicao(resultSet.getString("instituicao"));
        conta.setSaldoInicial(resultSet.getBigDecimal("saldo_inicial"));
        conta.setAtivo(resultSet.getBoolean("ativo"));
        conta.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        conta.setAtualizadoEm(toLocalDateTime(resultSet.getTimestamp("atualizado_em")));
        return conta;
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
