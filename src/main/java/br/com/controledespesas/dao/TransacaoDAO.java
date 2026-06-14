package br.com.controledespesas.dao;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;

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
 * Cross-entity business validations such as category ownership, account ownership,
 * category/transaction type compatibility, and complete status compatibility remain
 * mandatory, but they will be enforced in the service layer.
 */
public class TransacaoDAO {

    public Long inserir(Transacao transacao) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return inserir(connection, transacao);
        }
    }

    public Long inserir(Connection connection, Transacao transacao) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(transacao, "Transaction must not be null.");
        Objects.requireNonNull(transacao.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(transacao.getCategoriaId(), "Category ID must not be null.");
        Objects.requireNonNull(transacao.getContaId(), "Account ID must not be null.");
        Objects.requireNonNull(transacao.getTipo(), "Transaction type must not be null.");
        Objects.requireNonNull(transacao.getValor(), "Transaction value must not be null.");
        Objects.requireNonNull(transacao.getDataTransacao(), "Transaction date must not be null.");
        Objects.requireNonNull(transacao.getStatus(), "Transaction status must not be null.");

        String sql = """
                INSERT INTO transacoes (
                    usuario_id,
                    categoria_id,
                    conta_id,
                    tipo,
                    descricao,
                    valor,
                    data_transacao,
                    status,
                    observacoes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transacao.getUsuarioId());
            statement.setLong(2, transacao.getCategoriaId());
            statement.setLong(3, transacao.getContaId());
            statement.setString(4, transacao.getTipo().getValorBanco());
            statement.setString(5, normalizeRequiredText(transacao.getDescricao(), "Transaction description"));
            statement.setBigDecimal(6, transacao.getValor());
            statement.setDate(7, Date.valueOf(transacao.getDataTransacao()));
            statement.setString(8, transacao.getStatus().getValorBanco());
            statement.setString(9, normalizeOptionalText(transacao.getObservacoes()));

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting transaction failed because no rows were affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    transacao.setId(id);
                    return id;
                }
            }

            throw new SQLException("Inserting transaction failed because no generated key was returned.");
        }
    }

    public Optional<Transacao> buscarPorId(Long id, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return buscarPorId(connection, id, usuarioId);
        }
    }

    public Optional<Transacao> buscarPorId(Connection connection, Long id, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(id, "Transaction ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, categoria_id, conta_id, tipo, descricao, valor,
                       data_transacao, status, observacoes, criado_em, atualizado_em
                FROM transacoes
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapearTransacao(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public List<Transacao> listarPorUsuario(Long usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                SELECT id, usuario_id, categoria_id, conta_id, tipo, descricao, valor,
                       data_transacao, status, observacoes, criado_em, atualizado_em
                FROM transacoes
                WHERE usuario_id = ?
                ORDER BY data_transacao DESC, id DESC
                """;

        List<Transacao> transacoes = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, usuarioId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transacoes.add(mapearTransacao(resultSet));
                }
            }
        }

        return transacoes;
    }

    public List<Transacao> filtrar(Long usuarioId, TransacaoFiltro filtro) throws SQLException {
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        TransacaoFiltro filtroAplicado = filtro != null ? filtro : new TransacaoFiltro();
        StringBuilder sql = new StringBuilder("""
                SELECT id, usuario_id, categoria_id, conta_id, tipo, descricao, valor,
                       data_transacao, status, observacoes, criado_em, atualizado_em
                FROM transacoes
                WHERE usuario_id = ?
                """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(usuarioId);

        if (filtroAplicado.dataInicial() != null) {
            sql.append(" AND data_transacao >= ?");
            parametros.add(filtroAplicado.dataInicial());
        }
        if (filtroAplicado.dataFinal() != null) {
            sql.append(" AND data_transacao <= ?");
            parametros.add(filtroAplicado.dataFinal());
        }
        if (filtroAplicado.tipo() != null) {
            sql.append(" AND tipo = ?");
            parametros.add(filtroAplicado.tipo().getValorBanco());
        }
        if (filtroAplicado.status() != null) {
            sql.append(" AND status = ?");
            parametros.add(filtroAplicado.status().getValorBanco());
        }
        if (filtroAplicado.categoriaId() != null) {
            sql.append(" AND categoria_id = ?");
            parametros.add(filtroAplicado.categoriaId());
        }
        if (filtroAplicado.contaId() != null) {
            sql.append(" AND conta_id = ?");
            parametros.add(filtroAplicado.contaId());
        }
        if (filtroAplicado.descricao() != null) {
            sql.append(" AND LOWER(descricao) LIKE LOWER(?)");
            parametros.add("%" + filtroAplicado.descricao() + "%");
        }

        sql.append(" ORDER BY data_transacao DESC, id DESC");

        List<Transacao> transacoes = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            configurarParametros(statement, parametros);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transacoes.add(mapearTransacao(resultSet));
                }
            }
        }

        return transacoes;
    }

    public boolean atualizar(Transacao transacao) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return atualizar(connection, transacao);
        }
    }

    public boolean atualizar(Connection connection, Transacao transacao) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(transacao, "Transaction must not be null.");
        Objects.requireNonNull(transacao.getId(), "Transaction ID must not be null.");
        Objects.requireNonNull(transacao.getUsuarioId(), "User ID must not be null.");
        Objects.requireNonNull(transacao.getCategoriaId(), "Category ID must not be null.");
        Objects.requireNonNull(transacao.getContaId(), "Account ID must not be null.");
        Objects.requireNonNull(transacao.getTipo(), "Transaction type must not be null.");
        Objects.requireNonNull(transacao.getValor(), "Transaction value must not be null.");
        Objects.requireNonNull(transacao.getDataTransacao(), "Transaction date must not be null.");
        Objects.requireNonNull(transacao.getStatus(), "Transaction status must not be null.");

        String sql = """
                UPDATE transacoes
                SET categoria_id = ?,
                    conta_id = ?,
                    tipo = ?,
                    descricao = ?,
                    valor = ?,
                    data_transacao = ?,
                    status = ?,
                    observacoes = ?
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transacao.getCategoriaId());
            statement.setLong(2, transacao.getContaId());
            statement.setString(3, transacao.getTipo().getValorBanco());
            statement.setString(4, normalizeRequiredText(transacao.getDescricao(), "Transaction description"));
            statement.setBigDecimal(5, transacao.getValor());
            statement.setDate(6, Date.valueOf(transacao.getDataTransacao()));
            statement.setString(7, transacao.getStatus().getValorBanco());
            statement.setString(8, normalizeOptionalText(transacao.getObservacoes()));
            statement.setLong(9, transacao.getId());
            statement.setLong(10, transacao.getUsuarioId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean excluir(Long transacaoId, Long usuarioId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return excluir(connection, transacaoId, usuarioId);
        }
    }

    public boolean excluir(Connection connection, Long transacaoId, Long usuarioId) throws SQLException {
        Objects.requireNonNull(connection, "Connection must not be null.");
        Objects.requireNonNull(transacaoId, "Transaction ID must not be null.");
        Objects.requireNonNull(usuarioId, "User ID must not be null.");

        String sql = """
                DELETE FROM transacoes
                WHERE id = ?
                  AND usuario_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, transacaoId);
            statement.setLong(2, usuarioId);
            return statement.executeUpdate() > 0;
        }
    }

    public BigDecimal calcularTotalReceitas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal) throws SQLException {
        return calcularTotalPorTipoEStatus(usuarioId, TipoTransacao.RECEITA, StatusTransacao.RECEBIDO, dataInicial, dataFinal);
    }

    public BigDecimal calcularTotalDespesas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal) throws SQLException {
        return calcularTotalPorTipoEStatus(usuarioId, TipoTransacao.DESPESA, StatusTransacao.PAGO, dataInicial, dataFinal);
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

        if (dataInicial != null) {
            sql.append(" AND data_transacao >= ?");
            parametros.add(dataInicial);
        }
        if (dataFinal != null) {
            sql.append(" AND data_transacao <= ?");
            parametros.add(dataFinal);
        }

        try (Connection connection = DatabaseConnection.getConnection();
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

    private Transacao mapearTransacao(ResultSet resultSet) throws SQLException {
        Transacao transacao = new Transacao();
        transacao.setId(resultSet.getLong("id"));
        transacao.setUsuarioId(resultSet.getLong("usuario_id"));
        transacao.setCategoriaId(resultSet.getLong("categoria_id"));
        transacao.setContaId(resultSet.getLong("conta_id"));
        transacao.setTipo(TipoTransacao.fromValorBanco(resultSet.getString("tipo")));
        transacao.setDescricao(resultSet.getString("descricao"));
        transacao.setValor(resultSet.getBigDecimal("valor"));
        transacao.setDataTransacao(resultSet.getDate("data_transacao").toLocalDate());
        transacao.setStatus(StatusTransacao.fromValorBanco(resultSet.getString("status")));
        transacao.setObservacoes(resultSet.getString("observacoes"));
        transacao.setCriadoEm(toLocalDateTime(resultSet.getTimestamp("criado_em")));
        transacao.setAtualizadoEm(toLocalDateTime(resultSet.getTimestamp("atualizado_em")));
        return transacao;
    }

    private void configurarParametros(PreparedStatement statement, List<Object> parametros) throws SQLException {
        for (int index = 0; index < parametros.size(); index++) {
            Object parametro = parametros.get(index);
            int parameterIndex = index + 1;

            if (parametro instanceof Long valorLong) {
                statement.setLong(parameterIndex, valorLong);
            } else if (parametro instanceof String valorString) {
                statement.setString(parameterIndex, valorString);
            } else if (parametro instanceof LocalDate localDate) {
                statement.setDate(parameterIndex, Date.valueOf(localDate));
            } else {
                throw new IllegalArgumentException("Unsupported SQL parameter type: " + parametro.getClass().getName());
            }
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
