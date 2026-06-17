package br.com.controledespesas.service;

import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class MovimentacaoCofrinhoService {

    private static final int MAX_OBSERVACAO = 65535;

    private final MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;
    private final CofrinhoDAO cofrinhoDAO;
    private final ConnectionProvider connectionProvider;

    public MovimentacaoCofrinhoService() {
        this(new MovimentacaoCofrinhoDAO(), new CofrinhoDAO(), DatabaseConnection::getConnection);
    }

    public MovimentacaoCofrinhoService(MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO, CofrinhoDAO cofrinhoDAO,
                                       ConnectionProvider connectionProvider) {
        this.movimentacaoCofrinhoDAO =
                Objects.requireNonNull(movimentacaoCofrinhoDAO, "movimentacaoCofrinhoDAO nao pode ser nulo.");
        this.cofrinhoDAO = Objects.requireNonNull(cofrinhoDAO, "cofrinhoDAO nao pode ser nulo.");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
    }

    public MovimentacaoCofrinho depositar(Long cofrinhoId, Long usuarioId, BigDecimal valor,
                                          LocalDate dataMovimentacao, String observacao) throws SQLException {
        return registrarMovimentacao(
                cofrinhoId,
                usuarioId,
                TipoMovimentacaoCofrinho.DEPOSITO,
                valor,
                dataMovimentacao,
                observacao
        );
    }

    public MovimentacaoCofrinho retirar(Long cofrinhoId, Long usuarioId, BigDecimal valor,
                                        LocalDate dataMovimentacao, String observacao) throws SQLException {
        return registrarMovimentacao(
                cofrinhoId,
                usuarioId,
                TipoMovimentacaoCofrinho.RETIRADA,
                valor,
                dataMovimentacao,
                observacao
        );
    }

    public MovimentacaoCofrinho buscarPorId(Long movimentacaoId, Long usuarioId) throws SQLException {
        Long idMovimentacao = ServiceValidationUtils.requireId(movimentacaoId, "ID da movimentacao");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return movimentacaoCofrinhoDAO.buscarPorId(idMovimentacao, idUsuario)
                .orElseThrow(() -> new RegraNegocioException("Movimentacao nao encontrada."));
    }

    public List<MovimentacaoCofrinho> listarPorCofrinho(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCofrinhoExistente(idCofrinho, idUsuario);
        return movimentacaoCofrinhoDAO.listarPorCofrinho(idCofrinho, idUsuario);
    }

    public void excluir(Long movimentacaoId, Long usuarioId) throws SQLException {
        Long idMovimentacao = ServiceValidationUtils.requireId(movimentacaoId, "ID da movimentacao");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                MovimentacaoCofrinho movimentacao = movimentacaoCofrinhoDAO.buscarPorId(connection, idMovimentacao, idUsuario)
                        .orElseThrow(() -> new RegraNegocioException("Movimentacao nao encontrada."));

                Cofrinho cofrinho = cofrinhoDAO.buscarPorIdParaAtualizacao(connection, movimentacao.getCofrinhoId(), idUsuario)
                        .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));

                BigDecimal valorAtual = movimentacaoCofrinhoDAO.calcularValorAtual(connection, cofrinho.getId(), idUsuario)
                        .orElse(BigDecimal.ZERO);

                BigDecimal valorResultante = calcularValorResultanteExclusao(movimentacao, valorAtual);
                if (valorResultante.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RegraNegocioException(
                            "A movimentacao nao pode ser excluida porque deixaria o saldo do cofrinho negativo."
                    );
                }

                movimentacaoCofrinhoDAO.excluir(connection, idMovimentacao, idUsuario);
                atualizarStatusAutomatico(connection, cofrinho, valorResultante);

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                ServiceSqlUtils.rollback(connection, exception);
                throw exception;
            } finally {
                ServiceSqlUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private MovimentacaoCofrinho registrarMovimentacao(Long cofrinhoId, Long usuarioId, TipoMovimentacaoCofrinho tipo,
                                                       BigDecimal valor, LocalDate dataMovimentacao, String observacao)
            throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        TipoMovimentacaoCofrinho tipoMovimentacao = ServiceValidationUtils.requireValue(tipo, "Tipo da movimentacao");
        BigDecimal valorNormalizado = ServiceValidationUtils.normalizeMonetaryValue(valor, "Valor da movimentacao", false);
        LocalDate data = ServiceValidationUtils.requireDate(dataMovimentacao, "Data da movimentacao");
        String observacaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(observacao, "Observacao", MAX_OBSERVACAO);

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                Cofrinho cofrinho = cofrinhoDAO.buscarPorIdParaAtualizacao(connection, idCofrinho, idUsuario)
                        .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));

                if (cofrinho.getStatus() == StatusCofrinho.CANCELADO) {
                    throw new RegraNegocioException(
                            "O cofrinho informado esta cancelado e nao pode receber movimentacoes."
                    );
                }

                BigDecimal valorAtual = movimentacaoCofrinhoDAO.calcularValorAtual(connection, idCofrinho, idUsuario)
                        .orElse(BigDecimal.ZERO);

                if (tipoMovimentacao == TipoMovimentacaoCofrinho.RETIRADA
                        && valorNormalizado.compareTo(valorAtual) > 0) {
                    throw new RegraNegocioException(
                            "O valor da retirada nao pode ser maior que o valor disponivel no cofrinho."
                    );
                }

                MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho();
                movimentacao.setCofrinhoId(idCofrinho);
                movimentacao.setUsuarioId(idUsuario);
                movimentacao.setTipo(tipoMovimentacao);
                movimentacao.setValor(valorNormalizado);
                movimentacao.setDataMovimentacao(data);
                movimentacao.setObservacao(observacaoNormalizada);

                movimentacaoCofrinhoDAO.inserir(connection, movimentacao);

                BigDecimal novoValor = tipoMovimentacao == TipoMovimentacaoCofrinho.DEPOSITO
                        ? valorAtual.add(valorNormalizado)
                        : valorAtual.subtract(valorNormalizado);

                atualizarStatusAutomatico(connection, cofrinho, novoValor);
                connection.commit();
                return movimentacao;
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                ServiceSqlUtils.rollback(connection, exception);
                throw exception;
            } finally {
                ServiceSqlUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private Cofrinho buscarCofrinhoExistente(Long cofrinhoId, Long usuarioId) throws SQLException {
        return cofrinhoDAO.buscarPorId(cofrinhoId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));
    }

    private BigDecimal calcularValorResultanteExclusao(MovimentacaoCofrinho movimentacao, BigDecimal valorAtual) {
        return movimentacao.getTipo() == TipoMovimentacaoCofrinho.DEPOSITO
                ? valorAtual.subtract(movimentacao.getValor())
                : valorAtual.add(movimentacao.getValor());
    }

    private void atualizarStatusAutomatico(Connection connection, Cofrinho cofrinho, BigDecimal valorAtual)
            throws SQLException {
        if (cofrinho.getStatus() == StatusCofrinho.CANCELADO) {
            return;
        }

        StatusCofrinho novoStatus = valorAtual.compareTo(cofrinho.getValorMeta()) >= 0
                ? StatusCofrinho.CONCLUIDO
                : StatusCofrinho.EM_ANDAMENTO;

        if (cofrinho.getStatus() != novoStatus) {
            cofrinhoDAO.atualizarStatus(connection, cofrinho.getId(), cofrinho.getUsuarioId(), novoStatus);
            cofrinho.setStatus(novoStatus);
        }
    }
}
