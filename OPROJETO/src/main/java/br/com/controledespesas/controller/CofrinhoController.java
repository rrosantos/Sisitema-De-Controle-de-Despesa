package br.com.controledespesas.controller;

import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.dto.CofrinhoFiltro;
import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.dto.PrazoCofrinhoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.util.CofrinhoProgressCalculator;
import br.com.controledespesas.util.SqlExceptionUtils;
import br.com.controledespesas.util.ValidationUtils;
import br.com.controledespesas.view.CofrinhoViewSupport;
import br.com.controledespesas.view.contract.CofrinhoView;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CofrinhoController {

    private static final Logger LOGGER = Logger.getLogger(CofrinhoController.class.getName());
    private static final String MENSAGEM_ERRO_CARREGAR =
            "Nao foi possivel carregar os cofrinhos. Tente novamente.";
    private static final String MENSAGEM_ERRO_SALVAR =
            "Nao foi possivel salvar o cofrinho. Tente novamente.";
    private static final String MENSAGEM_ERRO_MOVIMENTACAO =
            "Nao foi possivel registrar a movimentacao. Tente novamente.";
    private static final String MENSAGEM_ERRO_ATUALIZAR =
            "Nao foi possivel atualizar o cofrinho. Tente novamente.";
    private static final String MENSAGEM_ERRO_EXCLUIR =
            "Nao foi possivel excluir o cofrinho. Tente novamente.";
    private static final String MENSAGEM_ERRO_HISTORICO =
            "Nao foi possivel carregar o historico do cofrinho. Tente novamente.";
    private static final String MENSAGEM_ERRO_EXCLUIR_MOVIMENTACAO =
            "Nao foi possivel excluir a movimentacao. Tente novamente.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Cofrinho cadastrado com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Cofrinho atualizado com sucesso.";
    private static final String MENSAGEM_CANCELAMENTO_SUCESSO = "Cofrinho cancelado com sucesso.";
    private static final String MENSAGEM_REATIVACAO_SUCESSO = "Cofrinho reativado com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Cofrinho excluido com sucesso.";
    private static final String MENSAGEM_DEPOSITO_SUCESSO = "Deposito registrado com sucesso.";
    private static final String MENSAGEM_RETIRADA_SUCESSO = "Retirada registrada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_MOVIMENTACAO_SUCESSO = "Movimentacao excluida com sucesso.";
    private static final int MAX_NOME = 150;
    private static final int MAX_DESCRICAO = 65535;
    private static final int MAX_OBSERVACAO = 65535;

    private final CofrinhoDAO cofrinhoDAO;
    private final MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;
    private final ConnectionProvider connectionProvider;
    private final SessaoUsuario sessaoUsuario;
    private final CofrinhoView cofrinhoView;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final DashboardRefreshNotifier dashboardRefreshNotifier;

    private List<CofrinhoResumo> todosResumos = List.of();
    private CofrinhoFiltro filtroAtual = new CofrinhoFiltro();
    private Long historicoCofrinhoAbertoId;

    public CofrinhoController(CofrinhoDAO cofrinhoDAO, MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO,
                              ConnectionProvider connectionProvider, SessaoUsuario sessaoUsuario, CofrinhoView cofrinhoView,
                              AsyncTaskExecutor asyncTaskExecutor) {
        this(
                cofrinhoDAO,
                movimentacaoCofrinhoDAO,
                connectionProvider,
                sessaoUsuario,
                cofrinhoView,
                asyncTaskExecutor,
                DashboardRefreshNotifier.NO_OP
        );
    }

    public CofrinhoController(CofrinhoDAO cofrinhoDAO, MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO,
                              ConnectionProvider connectionProvider, SessaoUsuario sessaoUsuario, CofrinhoView cofrinhoView,
                              AsyncTaskExecutor asyncTaskExecutor,
                              DashboardRefreshNotifier dashboardRefreshNotifier) {
        this.cofrinhoDAO = Objects.requireNonNull(cofrinhoDAO, "cofrinhoDAO nao pode ser nulo.");
        this.movimentacaoCofrinhoDAO =
                Objects.requireNonNull(movimentacaoCofrinhoDAO, "movimentacaoCofrinhoDAO nao pode ser nulo.");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.cofrinhoView = Objects.requireNonNull(cofrinhoView, "cofrinhoView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        this.dashboardRefreshNotifier = Objects.requireNonNull(
                dashboardRefreshNotifier,
                "dashboardRefreshNotifier nao pode ser nulo."
        );
        configurarAcoes();
    }

    public void carregar() {
        cofrinhoView.limparMensagem();
        carregarDados(null, false);
    }

    public void aplicarFiltros() {
        filtroAtual = cofrinhoView.obterFiltro();
        cofrinhoView.limparMensagem();
        aplicarResumosFiltrados(null);
    }

    public void limparFiltros() {
        cofrinhoView.limparFiltros();
        filtroAtual = new CofrinhoFiltro();
        cofrinhoView.limparMensagem();
        aplicarResumosFiltrados(null);
    }

    public void novoCofrinho() {
        cofrinhoView.limparMensagem();
        cofrinhoView.abrirFormularioCadastro(
                dados -> executarOperacaoFormularioCofrinho(() -> cadastrarCofrinho(dados))
        );
    }

    public void editar(CofrinhoResumo resumo) {
        if (resumo == null) {
            return;
        }

        cofrinhoView.limparMensagem();
        cofrinhoView.abrirFormularioEdicao(
                resumo.cofrinho(),
                dados -> executarOperacaoFormularioCofrinho(() -> atualizarCofrinho(resumo, dados))
        );
    }

    public void cancelar(CofrinhoResumo resumo) {
        if (resumo == null || !cofrinhoView.confirmarCancelamento(resumo.cofrinho())) {
            return;
        }

        cofrinhoView.limparMensagem();
        executarOperacaoPainel(() -> cancelarCofrinho(resumo), this::tratarErroPainelAtualizacao);
    }

    public void reativar(CofrinhoResumo resumo) {
        if (resumo == null || !cofrinhoView.confirmarReativacao(resumo.cofrinho())) {
            return;
        }

        cofrinhoView.limparMensagem();
        executarOperacaoPainel(() -> reativarCofrinho(resumo), this::tratarErroPainelAtualizacao);
    }

    public void excluir(CofrinhoResumo resumo) {
        if (resumo == null || !cofrinhoView.confirmarExclusao(resumo.cofrinho())) {
            return;
        }

        cofrinhoView.limparMensagem();
        executarOperacaoPainel(() -> excluirCofrinho(resumo), this::tratarErroPainelExclusao);
    }

    public void depositar(CofrinhoResumo resumo) {
        if (resumo == null) {
            return;
        }

        cofrinhoView.limparMensagem();
        cofrinhoView.abrirFormularioDeposito(
                resumo,
                dados -> executarOperacaoFormularioMovimentacao(() -> depositarNoCofrinho(resumo, dados))
        );
    }

    public void retirar(CofrinhoResumo resumo) {
        if (resumo == null) {
            return;
        }

        cofrinhoView.limparMensagem();
        cofrinhoView.abrirFormularioRetirada(
                resumo,
                dados -> executarOperacaoFormularioMovimentacao(() -> retirarDoCofrinho(resumo, dados))
        );
    }

    public void abrirHistorico(CofrinhoResumo resumo) {
        if (resumo == null) {
            return;
        }

        historicoCofrinhoAbertoId = resumo.cofrinho().getId();
        cofrinhoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                () -> carregarHistorico(resumo),
                resultado -> cofrinhoView.abrirHistorico(
                        resultado.resumo(),
                        resultado.movimentacoes(),
                        movimentacao -> excluirMovimentacao(resultado.resumo(), movimentacao)
                ),
                this::tratarErroHistorico,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    public void excluirMovimentacao(CofrinhoResumo resumo, MovimentacaoCofrinho movimentacao) {
        if (resumo == null || movimentacao == null || !cofrinhoView.confirmarExclusaoMovimentacao(movimentacao)) {
            return;
        }

        cofrinhoView.limparMensagem();
        cofrinhoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                () -> excluirMovimentacaoInterna(resumo, movimentacao),
                this::aplicarResultado,
                this::tratarErroHistoricoExclusao,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    private void configurarAcoes() {
        cofrinhoView.definirAcaoNovoCofrinho(this::novoCofrinho);
        cofrinhoView.definirAcaoFiltrar(this::aplicarFiltros);
        cofrinhoView.definirAcaoLimparFiltros(this::limparFiltros);
        cofrinhoView.definirAcaoEditar(this::editar);
        cofrinhoView.definirAcaoCancelar(this::cancelar);
        cofrinhoView.definirAcaoReativar(this::reativar);
        cofrinhoView.definirAcaoExcluir(this::excluir);
        cofrinhoView.definirAcaoDepositar(this::depositar);
        cofrinhoView.definirAcaoRetirar(this::retirar);
        cofrinhoView.definirAcaoHistorico(this::abrirHistorico);
    }

    private void carregarDados(String mensagemSucesso, boolean carregarHistoricoAberto) {
        cofrinhoView.exibirCarregamento(true);
        Long historicoId = carregarHistoricoAberto ? historicoCofrinhoAbertoId : null;
        asyncTaskExecutor.execute(
                () -> montarResultado(mensagemSucesso, historicoId),
                this::aplicarResultado,
                this::tratarErroCarregamento,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormularioCofrinho(Callable<ResultadoCarregamentoCofrinhos> operacao) {
        cofrinhoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    cofrinhoView.fecharFormularioCofrinho();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormularioCofrinho,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormularioMovimentacao(Callable<ResultadoCarregamentoCofrinhos> operacao) {
        cofrinhoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    cofrinhoView.fecharFormularioMovimentacao();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormularioMovimentacao,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoPainel(Callable<ResultadoCarregamentoCofrinhos> operacao, Consumer<Throwable> onError) {
        cofrinhoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                this::aplicarResultado,
                onError,
                () -> cofrinhoView.exibirCarregamento(false)
        );
    }

    private ResultadoCarregamentoCofrinhos cadastrarCofrinho(DadosCofrinhoForm dados) throws SQLException {
        cadastrar(
                sessaoUsuario.exigirUsuarioId(),
                dados.nome(),
                dados.descricao(),
                dados.valorMeta(),
                dados.dataLimite()
        );
        return montarResultado(MENSAGEM_CADASTRO_SUCESSO, null);
    }

    private ResultadoCarregamentoCofrinhos atualizarCofrinho(CofrinhoResumo resumo, DadosCofrinhoForm dados)
            throws SQLException {
        atualizar(
                resumo.cofrinho().getId(),
                sessaoUsuario.exigirUsuarioId(),
                dados.nome(),
                dados.descricao(),
                dados.valorMeta(),
                dados.dataLimite()
        );
        return montarResultado(MENSAGEM_EDICAO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos cancelarCofrinho(CofrinhoResumo resumo) throws SQLException {
        alterarStatus(
                resumo.cofrinho().getId(),
                sessaoUsuario.exigirUsuarioId(),
                StatusCofrinho.CANCELADO
        );
        return montarResultado(MENSAGEM_CANCELAMENTO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos reativarCofrinho(CofrinhoResumo resumo) throws SQLException {
        reativar(resumo.cofrinho().getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_REATIVACAO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos excluirCofrinho(CofrinhoResumo resumo) throws SQLException {
        excluirCofrinho(resumo.cofrinho().getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_EXCLUSAO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos depositarNoCofrinho(CofrinhoResumo resumo,
                                                               DadosMovimentacaoCofrinhoForm dados) throws SQLException {
        depositar(
                resumo.cofrinho().getId(),
                sessaoUsuario.exigirUsuarioId(),
                dados.valor(),
                dados.dataMovimentacao(),
                dados.observacao()
        );
        return montarResultado(MENSAGEM_DEPOSITO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos retirarDoCofrinho(CofrinhoResumo resumo,
                                                             DadosMovimentacaoCofrinhoForm dados) throws SQLException {
        retirar(
                resumo.cofrinho().getId(),
                sessaoUsuario.exigirUsuarioId(),
                dados.valor(),
                dados.dataMovimentacao(),
                dados.observacao()
        );
        return montarResultado(MENSAGEM_RETIRADA_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos excluirMovimentacaoInterna(CofrinhoResumo resumo,
                                                                      MovimentacaoCofrinho movimentacao)
            throws SQLException {
        excluirMovimentacao(movimentacao.getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_EXCLUSAO_MOVIMENTACAO_SUCESSO, resumo.cofrinho().getId());
    }

    private ResultadoHistorico carregarHistorico(CofrinhoResumo resumo) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        CofrinhoResumo resumoAtual = obterResumoPorId(resumo.cofrinho().getId());
        if (resumoAtual == null) {
            resumoAtual = montarResumo(buscarPorId(resumo.cofrinho().getId(), usuarioId), usuarioId);
        }
        List<MovimentacaoCofrinho> movimentacoes =
                listarMovimentacoesPorCofrinho(resumoAtual.cofrinho().getId(), usuarioId);
        return new ResultadoHistorico(resumoAtual, movimentacoes);
    }

    private ResultadoCarregamentoCofrinhos montarResultado(String mensagemSucesso, Long historicoCofrinhoId)
            throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        List<CofrinhoResumo> resumos = carregarResumos(usuarioId);
        List<MovimentacaoCofrinho> historicoMovimentacoes = null;

        if (historicoCofrinhoId != null && contemResumo(resumos, historicoCofrinhoId)) {
            historicoMovimentacoes = listarMovimentacoesPorCofrinho(historicoCofrinhoId, usuarioId);
        }

        return new ResultadoCarregamentoCofrinhos(resumos, mensagemSucesso, historicoCofrinhoId, historicoMovimentacoes);
    }

    private List<CofrinhoResumo> carregarResumos(Long usuarioId) throws SQLException {
        List<Cofrinho> cofrinhos = listarPorUsuario(usuarioId);
        List<CofrinhoResumo> resumos = new ArrayList<>();

        for (Cofrinho cofrinho : cofrinhos) {
            resumos.add(montarResumo(cofrinho, usuarioId));
        }

        return resumos;
    }

    private CofrinhoResumo montarResumo(Cofrinho cofrinho, Long usuarioId) throws SQLException {
        BigDecimal valorAtual = consultarValorAtual(cofrinho.getId(), usuarioId);
        BigDecimal percentual = CofrinhoProgressCalculator.calculateProgressPercentage(valorAtual, cofrinho.getValorMeta());
        return new CofrinhoResumo(cofrinho, valorAtual, percentual);
    }

    private Cofrinho cadastrar(Long usuarioId, String nome, String descricao, BigDecimal valorMeta, LocalDate dataLimite)
            throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ValidationUtils.normalizeRequiredText(nome, "Nome do cofrinho", MAX_NOME);
        String descricaoNormalizada =
                ValidationUtils.normalizeOptionalText(descricao, "Descricao do cofrinho", MAX_DESCRICAO);
        BigDecimal valorMetaNormalizado =
                ValidationUtils.normalizeMonetaryValue(valorMeta, "Valor da meta", false);

        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setUsuarioId(idUsuario);
        cofrinho.setNome(nomeNormalizado);
        cofrinho.setDescricao(descricaoNormalizada);
        cofrinho.setValorMeta(valorMetaNormalizado);
        cofrinho.setDataLimite(dataLimite);
        cofrinho.setStatus(StatusCofrinho.EM_ANDAMENTO);

        cofrinhoDAO.inserir(cofrinho);
        return cofrinho;
    }

    private Cofrinho buscarPorId(Long cofrinhoId, Long usuarioId) throws SQLException {
        return buscarCofrinhoExistente(cofrinhoId, usuarioId);
    }

    private List<Cofrinho> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        return cofrinhoDAO.listarPorUsuario(idUsuario);
    }

    private Cofrinho atualizar(Long cofrinhoId, Long usuarioId, String nome, String descricao, BigDecimal valorMeta,
                               LocalDate dataLimite) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ValidationUtils.normalizeRequiredText(nome, "Nome do cofrinho", MAX_NOME);
        String descricaoNormalizada =
                ValidationUtils.normalizeOptionalText(descricao, "Descricao do cofrinho", MAX_DESCRICAO);
        BigDecimal valorMetaNormalizado =
                ValidationUtils.normalizeMonetaryValue(valorMeta, "Valor da meta", false);

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                Cofrinho cofrinhoExistente = cofrinhoDAO.buscarPorIdParaAtualizacao(connection, idCofrinho, idUsuario)
                        .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));

                if (Objects.equals(cofrinhoExistente.getNome(), nomeNormalizado)
                        && Objects.equals(cofrinhoExistente.getDescricao(), descricaoNormalizada)
                        && Objects.equals(cofrinhoExistente.getValorMeta(), valorMetaNormalizado)
                        && Objects.equals(cofrinhoExistente.getDataLimite(), dataLimite)) {
                    connection.rollback();
                    return cofrinhoExistente;
                }

                cofrinhoExistente.setNome(nomeNormalizado);
                cofrinhoExistente.setDescricao(descricaoNormalizada);
                cofrinhoExistente.setValorMeta(valorMetaNormalizado);
                cofrinhoExistente.setDataLimite(dataLimite);
                cofrinhoExistente.setStatus(definirStatusAposAtualizacao(connection, cofrinhoExistente));

                cofrinhoDAO.atualizar(connection, cofrinhoExistente);
                connection.commit();
                return cofrinhoExistente;
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private void alterarStatus(Long cofrinhoId, Long usuarioId, StatusCofrinho status) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        StatusCofrinho statusCofrinho = ValidationUtils.requireValue(status, "Status do cofrinho");
        Cofrinho cofrinhoExistente = buscarCofrinhoExistente(idCofrinho, idUsuario);
        if (cofrinhoExistente.getStatus() == statusCofrinho) {
            return;
        }

        cofrinhoDAO.atualizarStatus(idCofrinho, idUsuario, statusCofrinho);
    }

    private void reativar(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                Cofrinho cofrinho = cofrinhoDAO.buscarPorIdParaAtualizacao(connection, idCofrinho, idUsuario)
                        .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));

                if (cofrinho.getStatus() != StatusCofrinho.CANCELADO) {
                    connection.rollback();
                    return;
                }

                StatusCofrinho novoStatus = recalcularStatus(connection, cofrinho);
                cofrinhoDAO.atualizarStatus(connection, idCofrinho, idUsuario, novoStatus);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                falha = exception;
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private void excluirCofrinho(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCofrinhoExistente(idCofrinho, idUsuario);
        cofrinhoDAO.excluir(idCofrinho, idUsuario);
    }

    private BigDecimal consultarValorAtual(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        return movimentacaoCofrinhoDAO.calcularValorAtual(idCofrinho, idUsuario)
                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));
    }

    private MovimentacaoCofrinho depositar(Long cofrinhoId, Long usuarioId, BigDecimal valor,
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

    private MovimentacaoCofrinho retirar(Long cofrinhoId, Long usuarioId, BigDecimal valor,
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

    private List<MovimentacaoCofrinho> listarMovimentacoesPorCofrinho(Long cofrinhoId, Long usuarioId)
            throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCofrinhoExistente(idCofrinho, idUsuario);
        return movimentacaoCofrinhoDAO.listarPorCofrinho(idCofrinho, idUsuario);
    }

    private void excluirMovimentacao(Long movimentacaoId, Long usuarioId) throws SQLException {
        Long idMovimentacao = ValidationUtils.requireId(movimentacaoId, "ID da movimentacao");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");

        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            Throwable falha = null;

            try {
                connection.setAutoCommit(false);

                MovimentacaoCofrinho movimentacao =
                        movimentacaoCofrinhoDAO.buscarPorId(connection, idMovimentacao, idUsuario)
                                .orElseThrow(() -> new RegraNegocioException("Movimentacao nao encontrada."));

                Cofrinho cofrinho =
                        cofrinhoDAO.buscarPorIdParaAtualizacao(connection, movimentacao.getCofrinhoId(), idUsuario)
                                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));

                BigDecimal valorAtual =
                        movimentacaoCofrinhoDAO.calcularValorAtual(connection, cofrinho.getId(), idUsuario)
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
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private MovimentacaoCofrinho registrarMovimentacao(Long cofrinhoId, Long usuarioId,
                                                       TipoMovimentacaoCofrinho tipo,
                                                       BigDecimal valor, LocalDate dataMovimentacao,
                                                       String observacao) throws SQLException {
        Long idCofrinho = ValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        TipoMovimentacaoCofrinho tipoMovimentacao =
                ValidationUtils.requireValue(tipo, "Tipo da movimentacao");
        BigDecimal valorNormalizado =
                ValidationUtils.normalizeMonetaryValue(valor, "Valor da movimentacao", false);
        LocalDate data = ValidationUtils.requireDate(dataMovimentacao, "Data da movimentacao");
        String observacaoNormalizada =
                ValidationUtils.normalizeOptionalText(observacao, "Observacao", MAX_OBSERVACAO);

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
                SqlExceptionUtils.rollback(connection, exception);
                throw exception;
            } finally {
                SqlExceptionUtils.restoreAutoCommit(connection, originalAutoCommit, falha);
            }
        }
    }

    private StatusCofrinho definirStatusAposAtualizacao(Connection connection, Cofrinho cofrinho) throws SQLException {
        if (cofrinho.getStatus() == StatusCofrinho.CANCELADO) {
            return StatusCofrinho.CANCELADO;
        }
        return recalcularStatus(connection, cofrinho);
    }

    private StatusCofrinho recalcularStatus(Connection connection, Cofrinho cofrinho) throws SQLException {
        BigDecimal valorAtual = movimentacaoCofrinhoDAO.calcularValorAtual(
                connection,
                cofrinho.getId(),
                cofrinho.getUsuarioId()
        ).orElse(BigDecimal.ZERO);
        return valorAtual.compareTo(cofrinho.getValorMeta()) >= 0
                ? StatusCofrinho.CONCLUIDO
                : StatusCofrinho.EM_ANDAMENTO;
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

    private void aplicarResultado(ResultadoCarregamentoCofrinhos resultado) {
        todosResumos = List.copyOf(resultado.resumos());
        aplicarResumosFiltrados(resultado.mensagemSucesso());

        if (resultado.historicoCofrinhoId() != null) {
            CofrinhoResumo resumoHistorico = obterResumoPorId(resultado.historicoCofrinhoId());
            if (resumoHistorico == null) {
                cofrinhoView.fecharHistorico();
                if (Objects.equals(historicoCofrinhoAbertoId, resultado.historicoCofrinhoId())) {
                    historicoCofrinhoAbertoId = null;
                }
            } else if (resultado.historicoMovimentacoes() != null) {
                historicoCofrinhoAbertoId = resultado.historicoCofrinhoId();
                cofrinhoView.atualizarHistorico(resumoHistorico, resultado.historicoMovimentacoes());
            }
        }
    }

    private void aplicarResumosFiltrados(String mensagemSucesso) {
        List<CofrinhoResumo> filtrados = filtrarResumos(todosResumos, filtroAtual);
        cofrinhoView.exibirCofrinhos(filtrados);
        cofrinhoView.exibirResumoGeral(
                calcularTotalGuardado(filtrados),
                contarPorStatus(filtrados, StatusCofrinho.EM_ANDAMENTO),
                contarPorStatus(filtrados, StatusCofrinho.CONCLUIDO),
                contarPorStatus(filtrados, StatusCofrinho.CANCELADO)
        );
        if (filtrados.isEmpty()) {
            cofrinhoView.exibirEstadoVazio();
        }
        if (mensagemSucesso != null && !mensagemSucesso.isBlank()) {
            cofrinhoView.exibirMensagemSucesso(mensagemSucesso);
            dashboardRefreshNotifier.marcarDashboardComoDesatualizado();
        }
    }

    private List<CofrinhoResumo> filtrarResumos(List<CofrinhoResumo> origem, CofrinhoFiltro filtro) {
        String pesquisa = filtro.pesquisa() != null ? filtro.pesquisa().toLowerCase(Locale.ROOT) : null;
        LocalDate hoje = LocalDate.now();
        List<CofrinhoResumo> filtrados = new ArrayList<>();

        for (CofrinhoResumo resumo : origem) {
            if (pesquisa != null && !resumo.cofrinho().getNome().toLowerCase(Locale.ROOT).contains(pesquisa)) {
                continue;
            }
            if (filtro.status() != null && resumo.cofrinho().getStatus() != filtro.status()) {
                continue;
            }
            if (!correspondeFiltroPrazo(resumo, filtro.prazo(), hoje)) {
                continue;
            }
            filtrados.add(resumo);
        }

        return filtrados;
    }

    private boolean correspondeFiltroPrazo(CofrinhoResumo resumo, PrazoCofrinhoFiltro prazo, LocalDate hoje) {
        PrazoCofrinhoFiltro filtroPrazo = prazo != null ? prazo : PrazoCofrinhoFiltro.TODOS;
        return switch (filtroPrazo) {
            case TODOS -> true;
            case COM_PRAZO -> resumo.cofrinho().getDataLimite() != null;
            case SEM_PRAZO -> resumo.cofrinho().getDataLimite() == null;
            case ATRASADOS -> CofrinhoViewSupport.estaAtrasado(resumo.cofrinho(), hoje);
        };
    }

    private BigDecimal calcularTotalGuardado(List<CofrinhoResumo> resumos) {
        BigDecimal total = BigDecimal.ZERO;
        for (CofrinhoResumo resumo : resumos) {
            total = total.add(resumo.valorAtual());
        }
        return total;
    }

    private int contarPorStatus(List<CofrinhoResumo> resumos, StatusCofrinho status) {
        int total = 0;
        for (CofrinhoResumo resumo : resumos) {
            if (resumo.cofrinho().getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    private boolean contemResumo(List<CofrinhoResumo> resumos, Long cofrinhoId) {
        return obterResumoPorId(resumos, cofrinhoId) != null;
    }

    private CofrinhoResumo obterResumoPorId(Long cofrinhoId) {
        return obterResumoPorId(todosResumos, cofrinhoId);
    }

    private CofrinhoResumo obterResumoPorId(List<CofrinhoResumo> resumos, Long cofrinhoId) {
        if (cofrinhoId == null) {
            return null;
        }

        for (CofrinhoResumo resumo : resumos) {
            if (Objects.equals(resumo.cofrinho().getId(), cofrinhoId)) {
                return resumo;
            }
        }
        return null;
    }

    private void tratarErroCarregamento(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao carregar cofrinhos.", throwable);
            cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_CARREGAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao carregar cofrinhos.", throwable);
        cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_CARREGAR);
    }

    private void tratarErroFormularioCofrinho(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirErroFormularioCofrinho(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao salvar cofrinho.", throwable);
            cofrinhoView.exibirErroFormularioCofrinho(MENSAGEM_ERRO_SALVAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao salvar cofrinho.", throwable);
        cofrinhoView.exibirErroFormularioCofrinho(MENSAGEM_ERRO_SALVAR);
    }

    private void tratarErroFormularioMovimentacao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirErroFormularioMovimentacao(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao registrar movimentacao de cofrinho.", throwable);
            cofrinhoView.exibirErroFormularioMovimentacao(MENSAGEM_ERRO_MOVIMENTACAO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao registrar movimentacao de cofrinho.", throwable);
        cofrinhoView.exibirErroFormularioMovimentacao(MENSAGEM_ERRO_MOVIMENTACAO);
    }

    private void tratarErroPainelAtualizacao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao atualizar cofrinho.", throwable);
            cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_ATUALIZAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao atualizar cofrinho.", throwable);
        cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_ATUALIZAR);
    }

    private void tratarErroPainelExclusao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao excluir cofrinho.", throwable);
            cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao excluir cofrinho.", throwable);
        cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
    }

    private void tratarErroHistorico(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirMensagemErro(throwable.getMessage());
            cofrinhoView.exibirErroHistorico(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao carregar historico do cofrinho.", throwable);
            cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_HISTORICO);
            cofrinhoView.exibirErroHistorico(MENSAGEM_ERRO_HISTORICO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao carregar historico do cofrinho.", throwable);
        cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_HISTORICO);
        cofrinhoView.exibirErroHistorico(MENSAGEM_ERRO_HISTORICO);
    }

    private void tratarErroHistoricoExclusao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            cofrinhoView.exibirErroHistorico(throwable.getMessage());
            cofrinhoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao excluir movimentacao do cofrinho.", throwable);
            cofrinhoView.exibirErroHistorico(MENSAGEM_ERRO_EXCLUIR_MOVIMENTACAO);
            cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR_MOVIMENTACAO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao excluir movimentacao do cofrinho.", throwable);
        cofrinhoView.exibirErroHistorico(MENSAGEM_ERRO_EXCLUIR_MOVIMENTACAO);
        cofrinhoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR_MOVIMENTACAO);
    }

    private record ResultadoCarregamentoCofrinhos(
            List<CofrinhoResumo> resumos,
            String mensagemSucesso,
            Long historicoCofrinhoId,
            List<MovimentacaoCofrinho> historicoMovimentacoes
    ) {
    }

    private record ResultadoHistorico(
            CofrinhoResumo resumo,
            List<MovimentacaoCofrinho> movimentacoes
    ) {
    }
}
