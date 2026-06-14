package br.com.controledespesas.service;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class TransacaoService {

    private static final int MAX_DESCRICAO = 255;
    private static final int MAX_OBSERVACOES = 65535;

    private final TransacaoDAO transacaoDAO;
    private final CategoriaDAO categoriaDAO;
    private final ContaDAO contaDAO;
    private final ConnectionProvider connectionProvider;

    public TransacaoService() {
        this(new TransacaoDAO(), new CategoriaDAO(), new ContaDAO(), DatabaseConnection::getConnection);
    }

    public TransacaoService(TransacaoDAO transacaoDAO, CategoriaDAO categoriaDAO, ContaDAO contaDAO,
                            ConnectionProvider connectionProvider) {
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO, "transacaoDAO nao pode ser nulo.");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO nao pode ser nulo.");
        this.contaDAO = Objects.requireNonNull(contaDAO, "contaDAO nao pode ser nulo.");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
    }

    public Transacao cadastrar(Long usuarioId, Long categoriaId, Long contaId, TipoTransacao tipo, String descricao,
                               BigDecimal valor, LocalDate dataTransacao, StatusTransacao status, String observacoes)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        Long idCategoria = ServiceValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        TipoTransacao tipoTransacao = ServiceValidationUtils.requireValue(tipo, "Tipo da transacao");
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeRequiredText(descricao, "Descricao da transacao", MAX_DESCRICAO);
        BigDecimal valorNormalizado = ServiceValidationUtils.normalizeMonetaryValue(valor, "Valor da transacao", false);
        LocalDate data = ServiceValidationUtils.requireDate(dataTransacao, "Data da transacao");
        StatusTransacao statusTransacao = ServiceValidationUtils.requireValue(status, "Status da transacao");
        String observacoesNormalizadas =
                ServiceValidationUtils.normalizeOptionalText(observacoes, "Observacoes", MAX_OBSERVACOES);

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                Categoria categoria = buscarCategoriaExistente(connection, idCategoria, idUsuario);
                validarCategoriaParaTransacao(categoria, tipoTransacao, true);

                Conta conta = buscarContaExistente(connection, idConta, idUsuario);
                validarContaParaTransacao(conta, true);

                validarStatusCompativel(tipoTransacao, statusTransacao);

                Transacao transacao = new Transacao();
                transacao.setUsuarioId(idUsuario);
                transacao.setCategoriaId(idCategoria);
                transacao.setContaId(idConta);
                transacao.setTipo(tipoTransacao);
                transacao.setDescricao(descricaoNormalizada);
                transacao.setValor(valorNormalizado);
                transacao.setDataTransacao(data);
                transacao.setStatus(statusTransacao);
                transacao.setObservacoes(observacoesNormalizadas);

                transacaoDAO.inserir(connection, transacao);
                connection.commit();
                return transacao;
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                ServiceSqlUtils.rollback(connection, exception);
                throw exception;
            } finally {
                ServiceSqlUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    public Transacao buscarPorId(Long transacaoId, Long usuarioId) throws SQLException {
        Long idTransacao = ServiceValidationUtils.requireId(transacaoId, "ID da transacao");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return buscarTransacaoExistente(idTransacao, idUsuario);
    }

    public List<Transacao> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return transacaoDAO.listarPorUsuario(idUsuario);
    }

    public List<Transacao> filtrar(Long usuarioId, TransacaoFiltro filtro) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        TransacaoFiltro filtroNormalizado = normalizarFiltro(filtro);
        ServiceValidationUtils.validateDateRange(filtroNormalizado.dataInicial(), filtroNormalizado.dataFinal());
        return transacaoDAO.filtrar(idUsuario, filtroNormalizado);
    }

    public Transacao atualizar(Long transacaoId, Long usuarioId, Long categoriaId, Long contaId, TipoTransacao tipo,
                               String descricao, BigDecimal valor, LocalDate dataTransacao, StatusTransacao status,
                               String observacoes) throws SQLException {
        Long idTransacao = ServiceValidationUtils.requireId(transacaoId, "ID da transacao");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        Long idCategoria = ServiceValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        TipoTransacao tipoTransacao = ServiceValidationUtils.requireValue(tipo, "Tipo da transacao");
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeRequiredText(descricao, "Descricao da transacao", MAX_DESCRICAO);
        BigDecimal valorNormalizado = ServiceValidationUtils.normalizeMonetaryValue(valor, "Valor da transacao", false);
        LocalDate data = ServiceValidationUtils.requireDate(dataTransacao, "Data da transacao");
        StatusTransacao statusTransacao = ServiceValidationUtils.requireValue(status, "Status da transacao");
        String observacoesNormalizadas =
                ServiceValidationUtils.normalizeOptionalText(observacoes, "Observacoes", MAX_OBSERVACOES);

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                Transacao transacaoExistente = buscarTransacaoExistente(connection, idTransacao, idUsuario);
                Categoria categoria = buscarCategoriaExistente(connection, idCategoria, idUsuario);
                validarCategoriaParaTransacao(
                        categoria,
                        tipoTransacao,
                        !Objects.equals(transacaoExistente.getCategoriaId(), idCategoria)
                );

                Conta conta = buscarContaExistente(connection, idConta, idUsuario);
                validarContaParaTransacao(
                        conta,
                        !Objects.equals(transacaoExistente.getContaId(), idConta)
                );

                validarStatusCompativel(tipoTransacao, statusTransacao);

                if (Objects.equals(transacaoExistente.getCategoriaId(), idCategoria)
                        && Objects.equals(transacaoExistente.getContaId(), idConta)
                        && transacaoExistente.getTipo() == tipoTransacao
                        && Objects.equals(transacaoExistente.getDescricao(), descricaoNormalizada)
                        && Objects.equals(transacaoExistente.getValor(), valorNormalizado)
                        && Objects.equals(transacaoExistente.getDataTransacao(), data)
                        && transacaoExistente.getStatus() == statusTransacao
                        && Objects.equals(transacaoExistente.getObservacoes(), observacoesNormalizadas)) {
                    connection.rollback();
                    return transacaoExistente;
                }

                transacaoExistente.setCategoriaId(idCategoria);
                transacaoExistente.setContaId(idConta);
                transacaoExistente.setTipo(tipoTransacao);
                transacaoExistente.setDescricao(descricaoNormalizada);
                transacaoExistente.setValor(valorNormalizado);
                transacaoExistente.setDataTransacao(data);
                transacaoExistente.setStatus(statusTransacao);
                transacaoExistente.setObservacoes(observacoesNormalizadas);

                transacaoDAO.atualizar(connection, transacaoExistente);
                connection.commit();
                return transacaoExistente;
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                ServiceSqlUtils.rollback(connection, exception);
                throw exception;
            } finally {
                ServiceSqlUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    public void excluir(Long transacaoId, Long usuarioId) throws SQLException {
        Long idTransacao = ServiceValidationUtils.requireId(transacaoId, "ID da transacao");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarTransacaoExistente(idTransacao, idUsuario);
        transacaoDAO.excluir(idTransacao, idUsuario);
    }

    public BigDecimal calcularTotalReceitas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        ServiceValidationUtils.validateDateRange(dataInicial, dataFinal);
        return transacaoDAO.calcularTotalReceitas(idUsuario, dataInicial, dataFinal);
    }

    public BigDecimal calcularTotalDespesas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        ServiceValidationUtils.validateDateRange(dataInicial, dataFinal);
        return transacaoDAO.calcularTotalDespesas(idUsuario, dataInicial, dataFinal);
    }

    public BigDecimal calcularSaldoDoPeriodo(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        ServiceValidationUtils.validateDateRange(dataInicial, dataFinal);
        BigDecimal totalReceitas = transacaoDAO.calcularTotalReceitas(idUsuario, dataInicial, dataFinal);
        BigDecimal totalDespesas = transacaoDAO.calcularTotalDespesas(idUsuario, dataInicial, dataFinal);
        return totalReceitas.subtract(totalDespesas);
    }

    private TransacaoFiltro normalizarFiltro(TransacaoFiltro filtro) {
        TransacaoFiltro filtroBase = filtro != null ? filtro : new TransacaoFiltro();
        validarIdOpcional(filtroBase.categoriaId(), "ID da categoria");
        validarIdOpcional(filtroBase.contaId(), "ID da conta");
        String descricao =
                ServiceValidationUtils.normalizeOptionalText(filtroBase.descricao(), "Descricao da transacao", MAX_DESCRICAO);

        return new TransacaoFiltro(
                filtroBase.dataInicial(),
                filtroBase.dataFinal(),
                filtroBase.tipo(),
                filtroBase.status(),
                filtroBase.categoriaId(),
                filtroBase.contaId(),
                descricao
        );
    }

    private void validarIdOpcional(Long valor, String nomeCampo) {
        if (valor != null) {
            ServiceValidationUtils.requireId(valor, nomeCampo);
        }
    }

    private Categoria buscarCategoriaExistente(Connection connection, Long categoriaId, Long usuarioId) throws SQLException {
        return categoriaDAO.buscarPorId(connection, categoriaId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Categoria nao encontrada."));
    }

    private Conta buscarContaExistente(Connection connection, Long contaId, Long usuarioId) throws SQLException {
        return contaDAO.buscarPorId(connection, contaId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Conta nao encontrada."));
    }

    private Transacao buscarTransacaoExistente(Long transacaoId, Long usuarioId) throws SQLException {
        return transacaoDAO.buscarPorId(transacaoId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Transacao nao encontrada."));
    }

    private Transacao buscarTransacaoExistente(Connection connection, Long transacaoId, Long usuarioId)
            throws SQLException {
        return transacaoDAO.buscarPorId(connection, transacaoId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Transacao nao encontrada."));
    }

    private void validarCategoriaParaTransacao(Categoria categoria, TipoTransacao tipoTransacao, boolean exigirAtiva) {
        if (exigirAtiva && !categoria.isAtivo()) {
            throw new RegraNegocioException("A categoria informada esta inativa.");
        }

        if (!categoria.getTipo().getValorBanco().equals(tipoTransacao.getValorBanco())) {
            throw new RegraNegocioException("O tipo da categoria deve ser compativel com o tipo da transacao.");
        }
    }

    private void validarContaParaTransacao(Conta conta, boolean exigirAtiva) {
        if (exigirAtiva && !conta.isAtivo()) {
            throw new RegraNegocioException("A conta informada esta inativa.");
        }
    }

    private void validarStatusCompativel(TipoTransacao tipoTransacao, StatusTransacao statusTransacao) {
        boolean compativel = switch (tipoTransacao) {
            case RECEITA -> statusTransacao == StatusTransacao.PENDENTE
                    || statusTransacao == StatusTransacao.RECEBIDO
                    || statusTransacao == StatusTransacao.CANCELADO;
            case DESPESA -> statusTransacao == StatusTransacao.PENDENTE
                    || statusTransacao == StatusTransacao.PAGO
                    || statusTransacao == StatusTransacao.CANCELADO;
        };

        if (!compativel) {
            throw new RegraNegocioException("O status informado nao e compativel com o tipo da transacao.");
        }
    }
}
