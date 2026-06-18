package br.com.controledespesas.dao;

import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.dto.ResumoContaDashboard;
import br.com.controledespesas.dto.TransacaoRecenteDashboard;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoTransacao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Realiza a persistencia JDBC e consultas relacionadas a Dashboard.
 */
public class DashboardDAO {

    private final ConnectionProvider connectionProvider;

    public DashboardDAO() {
        this(DatabaseConnection::getConnection);
    }

    public DashboardDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
    }

    public List<ResumoContaDashboard> listarSaldosPorConta(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT c.id,
                       c.nome,
                       c.tipo,
                       c.ativo,
                       c.saldo_inicial + COALESCE(m.saldo_movimentos, 0) AS saldo_atual
                FROM contas c
                LEFT JOIN (
                    SELECT t.conta_id,
                           t.usuario_id,
                           SUM(
                               CASE
                                   WHEN t.tipo = 'receita' AND t.status = 'recebido' THEN t.valor
                                   WHEN t.tipo = 'despesa' AND t.status = 'pago' THEN -t.valor
                                   ELSE 0
                               END
                           ) AS saldo_movimentos
                    FROM transacoes t
                    WHERE t.usuario_id = ?
                    GROUP BY t.conta_id, t.usuario_id
                ) m
                    ON m.conta_id = c.id
                   AND m.usuario_id = c.usuario_id
                WHERE c.usuario_id = ?
                ORDER BY c.ativo DESC, saldo_atual DESC, c.nome
                """;

        List<ResumoContaDashboard> contas = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contas.add(new ResumoContaDashboard(
                            resultSet.getLong("id"),
                            resultSet.getString("nome"),
                            TipoConta.fromValorBanco(resultSet.getString("tipo")),
                            resultSet.getBoolean("ativo"),
                            resultSet.getBigDecimal("saldo_atual")
                    ));
                }
            }
        }

        return contas;
    }

    public BigDecimal calcularReceitasPeriodo(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        return calcularTotalPorTipoEStatus(usuarioId, TipoTransacao.RECEITA, StatusTransacao.RECEBIDO, dataInicial, dataFinal);
    }

    public BigDecimal calcularDespesasPeriodo(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        return calcularTotalPorTipoEStatus(usuarioId, TipoTransacao.DESPESA, StatusTransacao.PAGO, dataInicial, dataFinal);
    }

    public List<ResumoCategoriaDashboard> listarDespesasPorCategoria(Long usuarioId, LocalDate dataInicial,
                                                                     LocalDate dataFinal, int limite)
            throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        StringBuilder sql = new StringBuilder("""
                SELECT c.id AS categoria_id,
                       c.nome AS nome_categoria,
                       COALESCE(SUM(t.valor), 0) AS total
                FROM categorias c
                INNER JOIN transacoes t
                    ON t.categoria_id = c.id
                   AND t.usuario_id = c.usuario_id
                WHERE c.usuario_id = ?
                  AND c.tipo = 'despesa'
                  AND t.tipo = 'despesa'
                  AND t.status = 'pago'
                """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(usuarioId);
        adicionarFiltroPeriodo(sql, parametros, dataInicial, dataFinal, "t.data_transacao");
        sql.append("""
                
                GROUP BY c.id, c.nome
                ORDER BY total DESC, c.nome
                LIMIT ?
                """);
        parametros.add(limite);

        List<ResumoCategoriaDashboard> categorias = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            configurarParametros(statement, parametros);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categorias.add(new ResumoCategoriaDashboard(
                            resultSet.getLong("categoria_id"),
                            resultSet.getString("nome_categoria"),
                            resultSet.getBigDecimal("total"),
                            BigDecimal.ZERO
                    ));
                }
            }
        }

        return categorias;
    }

    public List<TransacaoRecenteDashboard> listarTransacoesRecentes(Long usuarioId, LocalDate dataInicial,
                                                                    LocalDate dataFinal, int limite)
            throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        StringBuilder sql = new StringBuilder("""
                SELECT t.id,
                       t.data_transacao,
                       t.descricao,
                       t.tipo,
                       t.status,
                       t.valor,
                       c.nome AS categoria_nome,
                       co.nome AS conta_nome
                FROM transacoes t
                LEFT JOIN categorias c
                    ON c.id = t.categoria_id
                   AND c.usuario_id = t.usuario_id
                LEFT JOIN contas co
                    ON co.id = t.conta_id
                   AND co.usuario_id = t.usuario_id
                WHERE t.usuario_id = ?
                """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(usuarioId);
        adicionarFiltroPeriodo(sql, parametros, dataInicial, dataFinal, "t.data_transacao");
        sql.append("""
                
                ORDER BY t.data_transacao DESC, t.id DESC
                LIMIT ?
                """);
        parametros.add(limite);

        List<TransacaoRecenteDashboard> transacoes = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            configurarParametros(statement, parametros);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transacoes.add(new TransacaoRecenteDashboard(
                            resultSet.getLong("id"),
                            resultSet.getDate("data_transacao").toLocalDate(),
                            resultSet.getString("descricao"),
                            TipoTransacao.fromValorBanco(resultSet.getString("tipo")),
                            StatusTransacao.fromValorBanco(resultSet.getString("status")),
                            resultSet.getBigDecimal("valor"),
                            resultSet.getString("categoria_nome"),
                            resultSet.getString("conta_nome")
                    ));
                }
            }
        }

        return transacoes;
    }

    public List<ResumoCofrinhoDashboard> listarResumoCofrinhos(Long usuarioId, int limite) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT c.id AS cofrinho_id,
                       c.nome,
                       c.status,
                       c.valor_meta,
                       c.data_limite,
                       COALESCE(m.valor_atual, 0) AS valor_atual
                FROM cofrinhos c
                LEFT JOIN (
                    SELECT m.cofrinho_id,
                           m.usuario_id,
                           SUM(
                               CASE
                                   WHEN m.tipo = 'deposito' THEN m.valor
                                   WHEN m.tipo = 'retirada' THEN -m.valor
                                   ELSE 0
                               END
                           ) AS valor_atual
                    FROM movimentacoes_cofrinho m
                    WHERE m.usuario_id = ?
                    GROUP BY m.cofrinho_id, m.usuario_id
                ) m
                    ON m.cofrinho_id = c.id
                   AND m.usuario_id = c.usuario_id
                WHERE c.usuario_id = ?
                ORDER BY
                    CASE c.status
                        WHEN 'em_andamento' THEN 0
                        WHEN 'concluido' THEN 1
                        ELSE 2
                    END,
                    CASE
                        WHEN c.status = 'em_andamento' AND c.data_limite IS NULL THEN 1
                        ELSE 0
                    END,
                    c.data_limite,
                    c.nome
                LIMIT ?
                """;

        List<ResumoCofrinhoDashboard> cofrinhos = new ArrayList<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);
            statement.setLong(2, usuarioId);
            statement.setInt(3, limite);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Date dataLimite = resultSet.getDate("data_limite");
                    cofrinhos.add(new ResumoCofrinhoDashboard(
                            resultSet.getLong("cofrinho_id"),
                            resultSet.getString("nome"),
                            StatusCofrinho.fromValorBanco(resultSet.getString("status")),
                            resultSet.getBigDecimal("valor_atual"),
                            resultSet.getBigDecimal("valor_meta"),
                            BigDecimal.ZERO,
                            dataLimite != null ? dataLimite.toLocalDate() : null
                    ));
                }
            }
        }

        return cofrinhos;
    }

    public int contarTransacoesPendentes(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT COUNT(*) AS total
                FROM transacoes
                WHERE usuario_id = ?
                  AND status = 'pendente'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }
        }

        return 0;
    }

    private BigDecimal calcularTotalPorTipoEStatus(Long usuarioId, TipoTransacao tipo, StatusTransacao status,
                                                   LocalDate dataInicial, LocalDate dataFinal) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");
        Objects.requireNonNull(tipo, "Transaction type must not be null.");
        Objects.requireNonNull(status, "Transaction status must not be null.");

        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(valor), 0) AS total
                FROM transacoes
                WHERE usuario_id = ?
                  AND tipo = ?
                  AND status = ?
                """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(usuarioId);
        parametros.add(tipo.getValorBanco());
        parametros.add(status.getValorBanco());
        adicionarFiltroPeriodo(sql, parametros, dataInicial, dataFinal, "data_transacao");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            configurarParametros(statement, parametros);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    BigDecimal total = resultSet.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        }

        return BigDecimal.ZERO;
    }

    private void adicionarFiltroPeriodo(StringBuilder sql, List<Object> parametros, LocalDate dataInicial,
                                        LocalDate dataFinal, String colunaData) {
        if (dataInicial != null) {
            sql.append(" AND ").append(colunaData).append(" >= ?");
            parametros.add(dataInicial);
        }
        if (dataFinal != null) {
            sql.append(" AND ").append(colunaData).append(" <= ?");
            parametros.add(dataFinal);
        }
    }

    private void configurarParametros(PreparedStatement statement, List<Object> parametros) throws SQLException {
        for (int index = 0; index < parametros.size(); index++) {
            Object parametro = parametros.get(index);
            int parameterIndex = index + 1;

            if (parametro instanceof Long valorLong) {
                statement.setLong(parameterIndex, valorLong);
            } else if (parametro instanceof Integer valorInteger) {
                statement.setInt(parameterIndex, valorInteger);
            } else if (parametro instanceof String valorString) {
                statement.setString(parameterIndex, valorString);
            } else if (parametro instanceof LocalDate valorLocalDate) {
                statement.setDate(parameterIndex, Date.valueOf(valorLocalDate));
            } else {
                throw new IllegalArgumentException(
                        "Unsupported SQL parameter type: " + parametro.getClass().getName()
                );
            }
        }
    }
}
