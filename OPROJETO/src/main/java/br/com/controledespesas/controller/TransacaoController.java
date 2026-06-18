package br.com.controledespesas.controller;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.util.SqlExceptionUtils;
import br.com.controledespesas.util.ValidationUtils;
import br.com.controledespesas.view.contract.DadosTransacaoForm;
import br.com.controledespesas.view.contract.TransacaoView;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordena filtros, resumo e operacoes transacionais de receitas e despesas.
 */
public class TransacaoController {

    private static final Logger LOGGER = Logger.getLogger(TransacaoController.class.getName());
    private static final String MENSAGEM_ERRO_LISTAGEM =
            "Nao foi possivel carregar as transacoes. Tente novamente.";
    private static final String MENSAGEM_ERRO_SALVAR =
            "Nao foi possivel salvar a transacao. Tente novamente.";
    private static final String MENSAGEM_ERRO_EXCLUIR =
            "Nao foi possivel excluir a transacao. Tente novamente.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Transacao cadastrada com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Transacao atualizada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Transacao excluida com sucesso.";
    private static final int MAX_DESCRICAO = 255;
    private static final int MAX_OBSERVACOES = 65535;

    private final TransacaoDAO transacaoDAO;
    private final CategoriaDAO categoriaDAO;
    private final ContaDAO contaDAO;
    private final ConnectionProvider connectionProvider;
    private final SessaoUsuario sessaoUsuario;
    private final TransacaoView transacaoView;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final DashboardRefreshNotifier dashboardRefreshNotifier;

    private List<Categoria> categoriasUsuario = List.of();
    private List<Conta> contasUsuario = List.of();
    private TransacaoFiltro ultimoFiltroAplicado = new TransacaoFiltro();

    public TransacaoController(TransacaoDAO transacaoDAO, CategoriaDAO categoriaDAO, ContaDAO contaDAO,
                               ConnectionProvider connectionProvider, SessaoUsuario sessaoUsuario,
                               TransacaoView transacaoView, AsyncTaskExecutor asyncTaskExecutor) {
        this(
                transacaoDAO,
                categoriaDAO,
                contaDAO,
                connectionProvider,
                sessaoUsuario,
                transacaoView,
                asyncTaskExecutor,
                DashboardRefreshNotifier.NO_OP
        );
    }

    public TransacaoController(TransacaoDAO transacaoDAO, CategoriaDAO categoriaDAO, ContaDAO contaDAO,
                               ConnectionProvider connectionProvider, SessaoUsuario sessaoUsuario,
                               TransacaoView transacaoView, AsyncTaskExecutor asyncTaskExecutor,
                               DashboardRefreshNotifier dashboardRefreshNotifier) {
        this.transacaoDAO = Objects.requireNonNull(transacaoDAO, "transacaoDAO nao pode ser nulo.");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO nao pode ser nulo.");
        this.contaDAO = Objects.requireNonNull(contaDAO, "contaDAO nao pode ser nulo.");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.transacaoView = Objects.requireNonNull(transacaoView, "transacaoView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        this.dashboardRefreshNotifier = Objects.requireNonNull(
                dashboardRefreshNotifier,
                "dashboardRefreshNotifier nao pode ser nulo."
        );
        configurarAcoes();
    }

    public void carregar() {
        transacaoView.limparMensagem();
        carregarComFiltro(ultimoFiltroAplicado, null);
    }

    public void aplicarFiltros() {
        try {
            TransacaoFiltro filtro = transacaoView.obterFiltro();
            transacaoView.limparMensagem();
            carregarComFiltro(filtro, null);
        } catch (ValidacaoException exception) {
            transacaoView.exibirMensagemErro(exception.getMessage());
        }
    }

    public void limparFiltros() {
        transacaoView.limparFiltros();
        transacaoView.limparMensagem();
        carregarComFiltro(new TransacaoFiltro(), null);
    }

    public void novaTransacao() {
        transacaoView.limparMensagem();
        transacaoView.abrirFormularioCadastro(
                categoriasUsuario.stream().filter(Categoria::isAtivo).toList(),
                contasUsuario.stream().filter(Conta::isAtivo).toList(),
                dados -> executarOperacaoFormulario(() -> cadastrarTransacao(dados))
        );
    }

    public void editar(Transacao transacao) {
        if (transacao == null) {
            return;
        }

        transacaoView.limparMensagem();
        transacaoView.abrirFormularioEdicao(
                transacao,
                categoriasUsuario,
                contasUsuario,
                dados -> executarOperacaoFormulario(() -> atualizarTransacao(transacao, dados))
        );
    }

    public void excluir(Transacao transacao) {
        if (transacao == null || !transacaoView.confirmarExclusao(transacao)) {
            return;
        }

        transacaoView.limparMensagem();
        executarOperacaoExclusao(() -> excluirTransacao(transacao));
    }

    private void configurarAcoes() {
        transacaoView.definirAcaoNovaTransacao(this::novaTransacao);
        transacaoView.definirAcaoFiltrar(this::aplicarFiltros);
        transacaoView.definirAcaoLimparFiltros(this::limparFiltros);
        transacaoView.definirAcaoEditar(this::editar);
        transacaoView.definirAcaoExcluir(this::excluir);
    }

    private void carregarComFiltro(TransacaoFiltro filtro, String mensagemSucesso) {
        ultimoFiltroAplicado = filtro != null ? filtro : new TransacaoFiltro();
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                () -> carregarDados(ultimoFiltroAplicado, mensagemSucesso),
                this::aplicarResultado,
                this::tratarErroListagem,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormulario(Callable<ResultadoCarregamentoTransacoes> operacao) {
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    transacaoView.fecharFormulario();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormulario,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoExclusao(Callable<ResultadoCarregamentoTransacoes> operacao) {
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                this::aplicarResultado,
                this::tratarErroExclusao,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private ResultadoCarregamentoTransacoes cadastrarTransacao(DadosTransacaoForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        cadastrar(
                usuarioId,
                dados.categoriaId(),
                dados.contaId(),
                dados.tipo(),
                dados.descricao(),
                dados.valor(),
                dados.dataTransacao(),
                dados.status(),
                dados.observacoes()
        );
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_CADASTRO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes atualizarTransacao(Transacao transacao, DadosTransacaoForm dados)
            throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        atualizar(
                transacao.getId(),
                usuarioId,
                dados.categoriaId(),
                dados.contaId(),
                dados.tipo(),
                dados.descricao(),
                dados.valor(),
                dados.dataTransacao(),
                dados.status(),
                dados.observacoes()
        );
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_EDICAO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes excluirTransacao(Transacao transacao) throws SQLException {
        excluir(transacao.getId(), sessaoUsuario.exigirUsuarioId());
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_EXCLUSAO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes carregarDados(TransacaoFiltro filtro, String mensagemSucesso) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        TransacaoFiltro filtroNormalizado = normalizarFiltro(filtro);
        ValidationUtils.validateDateRange(filtroNormalizado.dataInicial(), filtroNormalizado.dataFinal());
        List<Categoria> categorias = categoriaDAO.listarPorUsuario(usuarioId);
        List<Conta> contas = contaDAO.listarPorUsuario(usuarioId);
        List<Transacao> transacoes = filtroNormalizado.possuiFiltros()
                ? filtrar(usuarioId, filtroNormalizado)
                : listarPorUsuario(usuarioId);
        BigDecimal totalReceitas = calcularTotalReceitas(
                usuarioId,
                filtroNormalizado.dataInicial(),
                filtroNormalizado.dataFinal()
        );
        BigDecimal totalDespesas = calcularTotalDespesas(
                usuarioId,
                filtroNormalizado.dataInicial(),
                filtroNormalizado.dataFinal()
        );
        BigDecimal saldoPeriodo = totalReceitas.subtract(totalDespesas);

        return new ResultadoCarregamentoTransacoes(
                transacoes,
                categorias,
                contas,
                totalReceitas,
                totalDespesas,
                saldoPeriodo,
                mensagemSucesso
        );
    }

    private void aplicarResultado(ResultadoCarregamentoTransacoes resultado) {
        categoriasUsuario = List.copyOf(resultado.categorias());
        contasUsuario = List.copyOf(resultado.contas());
        transacaoView.exibirDadosRelacionados(mapearCategorias(resultado.categorias()), mapearContas(resultado.contas()));
        transacaoView.exibirTransacoes(resultado.transacoes());
        transacaoView.exibirResumo(resultado.totalReceitas(), resultado.totalDespesas(), resultado.saldoPeriodo());
        if (resultado.transacoes().isEmpty()) {
            transacaoView.exibirEstadoVazio();
        }
        if (resultado.mensagemSucesso() != null && !resultado.mensagemSucesso().isBlank()) {
            transacaoView.exibirMensagemSucesso(resultado.mensagemSucesso());
            dashboardRefreshNotifier.marcarDashboardComoDesatualizado();
        }
    }

    private void tratarErroListagem(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao carregar transacoes.", throwable);
            transacaoView.exibirMensagemErro(MENSAGEM_ERRO_LISTAGEM);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao carregar transacoes.", throwable);
        transacaoView.exibirMensagemErro(MENSAGEM_ERRO_LISTAGEM);
    }

    private void tratarErroFormulario(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirErroFormulario(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao salvar transacao.", throwable);
            transacaoView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao salvar transacao.", throwable);
        transacaoView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
    }

    private void tratarErroExclusao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao excluir transacao.", throwable);
            transacaoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao excluir transacao.", throwable);
        transacaoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
    }

    private Map<Long, Categoria> mapearCategorias(List<Categoria> categorias) {
        Map<Long, Categoria> categoriasMap = new LinkedHashMap<>();
        for (Categoria categoria : categorias) {
            if (categoria.getId() != null) {
                categoriasMap.put(categoria.getId(), categoria);
            }
        }
        return categoriasMap;
    }

    private Map<Long, Conta> mapearContas(List<Conta> contas) {
        Map<Long, Conta> contasMap = new LinkedHashMap<>();
        for (Conta conta : contas) {
            if (conta.getId() != null) {
                contasMap.put(conta.getId(), conta);
            }
        }
        return contasMap;
    }

    private Transacao cadastrar(Long usuarioId, Long categoriaId, Long contaId, TipoTransacao tipo, String descricao,
                                BigDecimal valor, LocalDate dataTransacao, StatusTransacao status, String observacoes)
            throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        Long idCategoria = ValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idConta = ValidationUtils.requireId(contaId, "ID da conta");
        TipoTransacao tipoTransacao = ValidationUtils.requireValue(tipo, "Tipo da transacao");
        String descricaoNormalizada = ValidationUtils.normalizeRequiredText(descricao, "Descricao da transacao", MAX_DESCRICAO);
        BigDecimal valorNormalizado = ValidationUtils.normalizeMonetaryValue(valor, "Valor da transacao", false);
        LocalDate data = ValidationUtils.requireDate(dataTransacao, "Data da transacao");
        StatusTransacao statusTransacao = ValidationUtils.requireValue(status, "Status da transacao");
        String observacoesNormalizadas = ValidationUtils.normalizeOptionalText(observacoes, "Observacoes", MAX_OBSERVACOES);

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
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private Transacao atualizar(Long transacaoId, Long usuarioId, Long categoriaId, Long contaId, TipoTransacao tipo,
                                String descricao, BigDecimal valor, LocalDate dataTransacao, StatusTransacao status,
                                String observacoes) throws SQLException {
        Long idTransacao = ValidationUtils.requireId(transacaoId, "ID da transacao");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        Long idCategoria = ValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idConta = ValidationUtils.requireId(contaId, "ID da conta");
        TipoTransacao tipoTransacao = ValidationUtils.requireValue(tipo, "Tipo da transacao");
        String descricaoNormalizada = ValidationUtils.normalizeRequiredText(descricao, "Descricao da transacao", MAX_DESCRICAO);
        BigDecimal valorNormalizado = ValidationUtils.normalizeMonetaryValue(valor, "Valor da transacao", false);
        LocalDate data = ValidationUtils.requireDate(dataTransacao, "Data da transacao");
        StatusTransacao statusTransacao = ValidationUtils.requireValue(status, "Status da transacao");
        String observacoesNormalizadas = ValidationUtils.normalizeOptionalText(observacoes, "Observacoes", MAX_OBSERVACOES);

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
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private void excluir(Long transacaoId, Long usuarioId) throws SQLException {
        Long idTransacao = ValidationUtils.requireId(transacaoId, "ID da transacao");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarTransacaoExistente(idTransacao, idUsuario);
        transacaoDAO.excluir(idTransacao, idUsuario);
    }

    private List<Transacao> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        return transacaoDAO.listarPorUsuario(idUsuario);
    }

    private List<Transacao> filtrar(Long usuarioId, TransacaoFiltro filtro) throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        TransacaoFiltro filtroNormalizado = normalizarFiltro(filtro);
        ValidationUtils.validateDateRange(filtroNormalizado.dataInicial(), filtroNormalizado.dataFinal());
        return transacaoDAO.filtrar(idUsuario, filtroNormalizado);
    }

    private BigDecimal calcularTotalReceitas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        ValidationUtils.validateDateRange(dataInicial, dataFinal);
        return transacaoDAO.calcularTotalReceitas(idUsuario, dataInicial, dataFinal);
    }

    private BigDecimal calcularTotalDespesas(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal)
            throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        ValidationUtils.validateDateRange(dataInicial, dataFinal);
        return transacaoDAO.calcularTotalDespesas(idUsuario, dataInicial, dataFinal);
    }

    private TransacaoFiltro normalizarFiltro(TransacaoFiltro filtro) {
        TransacaoFiltro filtroBase = filtro != null ? filtro : new TransacaoFiltro();
        validarIdOpcional(filtroBase.categoriaId(), "ID da categoria");
        validarIdOpcional(filtroBase.contaId(), "ID da conta");
        String descricao = ValidationUtils.normalizeOptionalText(filtroBase.descricao(), "Descricao da transacao", MAX_DESCRICAO);

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
            ValidationUtils.requireId(valor, nomeCampo);
        }
    }

    private Categoria buscarCategoriaExistente(Connection connection, Long categoriaId, Long usuarioId)
            throws SQLException {
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

    private record ResultadoCarregamentoTransacoes(
            List<Transacao> transacoes,
            List<Categoria> categorias,
            List<Conta> contas,
            BigDecimal totalReceitas,
            BigDecimal totalDespesas,
            BigDecimal saldoPeriodo,
            String mensagemSucesso
    ) {
    }
}
