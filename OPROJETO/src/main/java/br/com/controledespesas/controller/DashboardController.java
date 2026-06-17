package br.com.controledespesas.controller;

import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.service.DashboardService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.DashboardView;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController implements DashboardRefreshNotifier {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String MENSAGEM_ERRO_CARREGAMENTO =
            "Nao foi possivel carregar o resumo financeiro. Tente novamente.";

    private final DashboardService dashboardService;
    private final SessaoUsuario sessaoUsuario;
    private final DashboardView dashboardView;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final Clock clock;

    private boolean inicializado;
    private boolean carregando;
    private boolean desatualizado = true;
    private boolean marcadoComoDesatualizadoDuranteCarregamento;
    private DashboardResumo ultimoResumo;
    private Runnable abrirTransacoesAction = () -> {
    };
    private Runnable abrirContasAction = () -> {
    };
    private Runnable abrirCofrinhosAction = () -> {
    };

    public DashboardController(DashboardService dashboardService, SessaoUsuario sessaoUsuario,
                               DashboardView dashboardView, AsyncTaskExecutor asyncTaskExecutor) {
        this(dashboardService, sessaoUsuario, dashboardView, asyncTaskExecutor, Clock.systemDefaultZone());
    }

    DashboardController(DashboardService dashboardService, SessaoUsuario sessaoUsuario,
                        DashboardView dashboardView, AsyncTaskExecutor asyncTaskExecutor, Clock clock) {
        this.dashboardService = Objects.requireNonNull(dashboardService, "dashboardService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.dashboardView = Objects.requireNonNull(dashboardView, "dashboardView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        this.clock = Objects.requireNonNull(clock, "clock nao pode ser nulo.");
        configurarAcoes();
    }

    public void inicializar() {
        if (inicializado) {
            return;
        }

        dashboardView.definirPeriodo(primeiroDiaMesAtual(), hoje());
        dashboardView.limparErro();
        desatualizado = true;
        inicializado = true;
    }

    public void carregar() {
        try {
            LocalDate dataInicial = dashboardView.obterDataInicial();
            LocalDate dataFinal = dashboardView.obterDataFinal();
            validarPeriodo(dataInicial, dataFinal);
            carregarResumo(dataInicial, dataFinal);
        } catch (ValidacaoException exception) {
            dashboardView.exibirMensagemErro(exception.getMessage());
        }
    }

    public void aplicarFiltro() {
        carregar();
    }

    public void mostrarMesAtual() {
        dashboardView.definirPeriodo(primeiroDiaMesAtual(), hoje());
        carregar();
    }

    public void mostrarMesAnterior() {
        LocalDate referencia = hoje().minusMonths(1);
        dashboardView.definirPeriodo(referencia.withDayOfMonth(1), referencia.withDayOfMonth(referencia.lengthOfMonth()));
        carregar();
    }

    public void mostrarUltimosTrintaDias() {
        LocalDate dataFinal = hoje();
        dashboardView.definirPeriodo(dataFinal.minusDays(29), dataFinal);
        carregar();
    }

    public void mostrarEsteAno() {
        LocalDate dataFinal = hoje();
        dashboardView.definirPeriodo(dataFinal.withDayOfYear(1), dataFinal);
        carregar();
    }

    public void limparPeriodo() {
        dashboardView.definirPeriodo(null, null);
        carregar();
    }

    @Override
    public void marcarDashboardComoDesatualizado() {
        if (carregando) {
            marcadoComoDesatualizadoDuranteCarregamento = true;
            return;
        }
        desatualizado = true;
    }

    public void atualizarSeNecessario() {
        if (!inicializado) {
            inicializar();
        }
        if (desatualizado && !carregando) {
            carregar();
        }
    }

    public void definirAcaoAbrirTransacoes(Runnable acao) {
        abrirTransacoesAction = acao != null ? acao : () -> {
        };
    }

    public void definirAcaoAbrirContas(Runnable acao) {
        abrirContasAction = acao != null ? acao : () -> {
        };
    }

    public void definirAcaoAbrirCofrinhos(Runnable acao) {
        abrirCofrinhosAction = acao != null ? acao : () -> {
        };
    }

    public void abrirTransacoes() {
        abrirTransacoesAction.run();
    }

    public void abrirContas() {
        abrirContasAction.run();
    }

    public void abrirCofrinhos() {
        abrirCofrinhosAction.run();
    }

    private void configurarAcoes() {
        dashboardView.definirAcaoAplicarFiltro(this::aplicarFiltro);
        dashboardView.definirAcaoAtualizar(this::carregar);
        dashboardView.definirAcaoTentarNovamente(this::carregar);
        dashboardView.definirAcaoMesAtual(this::mostrarMesAtual);
        dashboardView.definirAcaoMesAnterior(this::mostrarMesAnterior);
        dashboardView.definirAcaoUltimosTrintaDias(this::mostrarUltimosTrintaDias);
        dashboardView.definirAcaoEsteAno(this::mostrarEsteAno);
        dashboardView.definirAcaoLimparPeriodo(this::limparPeriodo);
        dashboardView.definirAcaoAbrirTransacoes(this::abrirTransacoes);
        dashboardView.definirAcaoAbrirContas(this::abrirContas);
        dashboardView.definirAcaoAbrirCofrinhos(this::abrirCofrinhos);
    }

    private void carregarResumo(LocalDate dataInicial, LocalDate dataFinal) {
        if (carregando) {
            return;
        }

        dashboardView.limparErro();
        carregando = true;
        desatualizado = false;
        marcadoComoDesatualizadoDuranteCarregamento = false;
        dashboardView.exibirCarregamento(true);

        asyncTaskExecutor.execute(
                () -> carregarResumoInterno(dataInicial, dataFinal),
                this::aplicarResumo,
                this::tratarErro,
                this::finalizarCarregamento
        );
    }

    private DashboardResumo carregarResumoInterno(LocalDate dataInicial, LocalDate dataFinal) throws SQLException {
        return dashboardService.carregar(sessaoUsuario.exigirUsuarioId(), dataInicial, dataFinal);
    }

    private void aplicarResumo(DashboardResumo resumo) {
        ultimoResumo = resumo;
        dashboardView.exibirResumo(resumo);
        dashboardView.limparErro();
        desatualizado = marcadoComoDesatualizadoDuranteCarregamento;
    }

    private void tratarErro(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            dashboardView.exibirMensagemErro(throwable.getMessage());
            desatualizado = true;
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao carregar dashboard financeiro.", throwable);
            dashboardView.exibirMensagemErro(MENSAGEM_ERRO_CARREGAMENTO);
            desatualizado = true;
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao carregar dashboard financeiro.", throwable);
        dashboardView.exibirMensagemErro(MENSAGEM_ERRO_CARREGAMENTO);
        desatualizado = true;
    }

    private void finalizarCarregamento() {
        carregando = false;
        dashboardView.exibirCarregamento(false);
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new ValidacaoException("A data inicial nao pode ser posterior a data final.");
        }
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private LocalDate primeiroDiaMesAtual() {
        return hoje().withDayOfMonth(1);
    }
}
