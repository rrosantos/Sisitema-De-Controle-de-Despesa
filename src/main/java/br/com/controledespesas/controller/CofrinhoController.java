package br.com.controledespesas.controller;

import br.com.controledespesas.dto.CofrinhoFiltro;
import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.dto.PrazoCofrinhoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.service.CofrinhoService;
import br.com.controledespesas.service.MovimentacaoCofrinhoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.util.CofrinhoProgressCalculator;
import br.com.controledespesas.view.CofrinhoViewSupport;
import br.com.controledespesas.view.contract.CofrinhoView;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;

import java.math.BigDecimal;
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

    private final CofrinhoService cofrinhoService;
    private final MovimentacaoCofrinhoService movimentacaoCofrinhoService;
    private final SessaoUsuario sessaoUsuario;
    private final CofrinhoView cofrinhoView;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final DashboardRefreshNotifier dashboardRefreshNotifier;

    private List<CofrinhoResumo> todosResumos = List.of();
    private CofrinhoFiltro filtroAtual = new CofrinhoFiltro();
    private Long historicoCofrinhoAbertoId;

    public CofrinhoController(CofrinhoService cofrinhoService, MovimentacaoCofrinhoService movimentacaoCofrinhoService,
                              SessaoUsuario sessaoUsuario, CofrinhoView cofrinhoView,
                              AsyncTaskExecutor asyncTaskExecutor) {
        this(
                cofrinhoService,
                movimentacaoCofrinhoService,
                sessaoUsuario,
                cofrinhoView,
                asyncTaskExecutor,
                DashboardRefreshNotifier.NO_OP
        );
    }

    public CofrinhoController(CofrinhoService cofrinhoService, MovimentacaoCofrinhoService movimentacaoCofrinhoService,
                              SessaoUsuario sessaoUsuario, CofrinhoView cofrinhoView,
                              AsyncTaskExecutor asyncTaskExecutor,
                              DashboardRefreshNotifier dashboardRefreshNotifier) {
        this.cofrinhoService = Objects.requireNonNull(cofrinhoService, "cofrinhoService nao pode ser nulo.");
        this.movimentacaoCofrinhoService = Objects.requireNonNull(
                movimentacaoCofrinhoService,
                "movimentacaoCofrinhoService nao pode ser nulo."
        );
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
        cofrinhoService.cadastrar(
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
        cofrinhoService.atualizar(
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
        cofrinhoService.alterarStatus(
                resumo.cofrinho().getId(),
                sessaoUsuario.exigirUsuarioId(),
                StatusCofrinho.CANCELADO
        );
        return montarResultado(MENSAGEM_CANCELAMENTO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos reativarCofrinho(CofrinhoResumo resumo) throws SQLException {
        cofrinhoService.reativar(resumo.cofrinho().getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_REATIVACAO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos excluirCofrinho(CofrinhoResumo resumo) throws SQLException {
        cofrinhoService.excluir(resumo.cofrinho().getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_EXCLUSAO_SUCESSO, historicoCofrinhoAbertoId);
    }

    private ResultadoCarregamentoCofrinhos depositarNoCofrinho(CofrinhoResumo resumo,
                                                               DadosMovimentacaoCofrinhoForm dados) throws SQLException {
        movimentacaoCofrinhoService.depositar(
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
        movimentacaoCofrinhoService.retirar(
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
        movimentacaoCofrinhoService.excluir(movimentacao.getId(), sessaoUsuario.exigirUsuarioId());
        return montarResultado(MENSAGEM_EXCLUSAO_MOVIMENTACAO_SUCESSO, resumo.cofrinho().getId());
    }

    private ResultadoHistorico carregarHistorico(CofrinhoResumo resumo) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        CofrinhoResumo resumoAtual = obterResumoPorId(resumo.cofrinho().getId());
        if (resumoAtual == null) {
            resumoAtual = montarResumo(cofrinhoService.buscarPorId(resumo.cofrinho().getId(), usuarioId), usuarioId);
        }
        List<MovimentacaoCofrinho> movimentacoes =
                movimentacaoCofrinhoService.listarPorCofrinho(resumoAtual.cofrinho().getId(), usuarioId);
        return new ResultadoHistorico(resumoAtual, movimentacoes);
    }

    private ResultadoCarregamentoCofrinhos montarResultado(String mensagemSucesso, Long historicoCofrinhoId)
            throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        List<CofrinhoResumo> resumos = carregarResumos(usuarioId);
        List<MovimentacaoCofrinho> historicoMovimentacoes = null;

        if (historicoCofrinhoId != null && contemResumo(resumos, historicoCofrinhoId)) {
            historicoMovimentacoes = movimentacaoCofrinhoService.listarPorCofrinho(historicoCofrinhoId, usuarioId);
        }

        return new ResultadoCarregamentoCofrinhos(resumos, mensagemSucesso, historicoCofrinhoId, historicoMovimentacoes);
    }

    private List<CofrinhoResumo> carregarResumos(Long usuarioId) throws SQLException {
        List<Cofrinho> cofrinhos = cofrinhoService.listarPorUsuario(usuarioId);
        List<CofrinhoResumo> resumos = new ArrayList<>();

        for (Cofrinho cofrinho : cofrinhos) {
            resumos.add(montarResumo(cofrinho, usuarioId));
        }

        return resumos;
    }

    private CofrinhoResumo montarResumo(Cofrinho cofrinho, Long usuarioId) throws SQLException {
        BigDecimal valorAtual = cofrinhoService.consultarValorAtual(cofrinho.getId(), usuarioId);
        BigDecimal percentual = CofrinhoProgressCalculator.calculateProgressPercentage(valorAtual, cofrinho.getValorMeta());
        return new CofrinhoResumo(cofrinho, valorAtual, percentual);
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
