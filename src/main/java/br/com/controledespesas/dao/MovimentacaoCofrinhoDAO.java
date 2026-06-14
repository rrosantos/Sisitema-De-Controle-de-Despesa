package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import br.com.controledespesas.util.CofrinhoProgressCalculator;

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

/**
 * Ownership between usuario_id and cofrinho_id is enforced on insertion to avoid
 * linking a user's movement to another user's savings goal.
 */
public class MovimentacaoCofrinhoDAO {

    public Long inserir(MovimentacaoCofrinho movimentacao) throws SQLException {
        Objects.requireNonNull(movimentacao, "Savings goal movement must not be null.");
        Objects.requireNonNull(movimentacao.getCofrinhoId(), "Savings goal ID must not be null.");
        Objects.requireNonNull(movimentacao.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(movimentacao.getTipo(), "Movement type must not be null.");
        Objects.requireNonNull(movimentacao.getValor(), "Movement value must not be null.");
        Objects.requireNonNull(movimentacao.getDataMovimentacao(), "Movement date must not be null.");

        String sql = """
                INSERT INTO movimentacoes_cofrinho (
                    cofrinho_id,
                    usuario_id,
                    tipo,
                    valor,
                    data_movimentacao,
                    observacao
                )
                SELECT ?, ?, ?, ?, ?, ?
                FROM cofrinhos
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, movimentacao.getCofrinhoId());
            statement.setLong(2, movimentacao.getUsuarioId());
            statement.setString(3, movimentacao.getTipo().getValorBanco());
            statement.setBigDecimal(4, movimentacao.getValor());
            statement.setDate(5, Date.valueOf(movimentacao.getDataMovimentacao()));
            statement.setString(6, normalizeOptionalText(movimentacao.getObservacao()));
            statement.setLong(7, movimentacao.getCofrinhoId());
            statement.setLong(8, movimentacao.getUsuarioId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting savings goal movement failed because the savings goal does not belong to the informed user.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    movimentacao.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting savings goal movement failed because no generated key was returned.");
        }
    }

    public Optional<MovimentacaoCofrinho> buscarPorId(Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(id, "Movement ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, cofrinho_id, usuario_id, tipo, valor, data_movimentacao, observacao, criado_em
                FROM movimentacoes_cofrinho
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearMovimentacao(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<MovimentacaoCofrinho> listarPorCofrinho(Long cofrinhoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(cofrinhoId, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, cofrinho_id, usuario_id, tipo, valor, data_movimentacao, observacao, criado_em
                FROM movimentacoes_cofrinho
                WHERE cofrinho_id = ?
                  AND usuario_id = ?
                ORDER BY data_movimentacao DESC, id DESC
                """;

        List<MovimentacaoCofrinho> movimentacoes = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cofrinhoId);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    movimentacoes.add(mapearMovimentacao(resultSet));
                }
            }
        }

        return movimentacoes;
    }

    public boolean excluir(Long movimentacaoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(movimentacaoId, "Movement ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                DELETE FROM movimentacoes_cofrinho
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movimentacaoId);
            statement.setLong(2, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<BigDecimal> calcularValorAtual(Long cofrinhoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(cofrinhoId, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT COALESCE(SUM(
                           CASE
                               WHEN m.tipo = 'deposito' THEN m.valor
                               WHEN m.tipo = 'retirada' THEN -m.valor
                               ELSE 0
                           END
                       ), 0) AS valor_atual
                FROM cofrinhos c
                LEFT JOIN movimentacoes_cofrinho m
                    ON m.cofrinho_id = c.id
                   AND m.usuario_id = c.usuario_id
                WHERE c.id = ?
                  AND c.usuario_id = ?
                GROUP BY c.id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cofrinhoId);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getBigDecimal("valor_atual"));
                }
            }
        }

        return Optional.empty();
    }

    public Optional<BigDecimal> calcularPercentualProgresso(Long cofrinhoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(cofrinhoId, "Savings goal ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT c.valor_meta,
                       COALESCE(SUM(
                           CASE
                               WHEN m.tipo = 'deposito' THEN m.valor
                               WHEN m.tipo = 'retirada' THEN -m.valor
                               ELSE 0
                           END
                       ), 0) AS valor_atual
                FROM cofrinhos c
                LEFT JOIN movimentacoes_cofrinho m
                    ON m.cofrinho_id = c.id
                   AND m.usuario_id = c.usuario_id
                WHERE c.id = ?
                  AND c.usuario_id = ?
                GROUP BY c.id, c.valor_meta
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cofrinhoId);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    BigDecimal valorMeta = resultSet.getBigDecimal("valor_meta");
                    BigDecimal valorAtual = resultSet.getBigDecimal("valor_atual");
                    return Optional.of(CofrinhoProgressCalculator.calculateProgressPercentage(valorAtual, valorMeta));
                }
            }
        }

        return Optional.empty();
    }

    private MovimentacaoCofrinho mapearMovimentacao(ResultSet resultSet) throws SQLException {
        MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho();
        movimentacao.setId(resultSet.getLong("id"));
        movimentacao.setCofrinhoId(resultSet.getLong("cofrinho_id"));
        movimentacao.setUsuarioId(resultSet.getLong("usuario_id"));
        movimentacao.setTipo(TipoMovimentacaoCofrinho.fromValorBanco(resultSet.getString("tipo")));
        movimentacao.setValor(resultSet.getBigDecimal("valor"));
        movimentacao.setDataMovimentacao(resultSet.getDate("data_movimentacao").toLocalDate());
        movimentacao.setObservacao(resultSet.getString("observacao"));
        movimentacao.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        return movimentacao;
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
