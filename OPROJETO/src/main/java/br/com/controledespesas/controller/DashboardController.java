package br.com.controledespesas.controller;

import br.com.controledespesas.dao.DashboardDAO;
import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.dto.ResumoContaDashboard;
import br.com.controledespesas.dto.TransacaoRecenteDashboard;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.util.CofrinhoProgressCalculator;
import br.com.controledespesas.view.contract.DashboardView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordena filtros e carregamento assíncrono do resumo financeiro do dashboard.
 */
public class DashboardController implements DashboardRefreshNotifier {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String MENSAGEM_ERRO_CARREGAMENTO =
            "Nao foi possivel carregar o resumo financeiro. Tente novamente.";
    private static final int LIMITE_CATEGORIAS = 5;
    private static final int LIMITE_TRANSACOES_RECENTES = 5;
    private static final int LIMITE_COFRINHOS = 4;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final DashboardDAO dashboardDAO;
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

    public DashboardController(DashboardDAO dashboardDAO, SessaoUsuario sessaoUsuario,
                               DashboardView dashboardView, AsyncTaskExecutor asyncTaskExecutor) {
        this(dashboardDAO, sessaoUsuario, dashboardView, asyncTaskExecutor, Clock.systemDefaultZone());
    }

    DashboardController(DashboardDAO dashboardDAO, SessaoUsuario sessaoUsuario,
                        DashboardView dashboardView, AsyncTaskExecutor asyncTaskExecutor, Clock clock) {
        this.dashboardDAO = Objects.requireNonNull(dashboardDAO, "dashboardDAO nao pode ser nulo.");
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
        Long usuarioId = requireId(sessaoUsuario.exigirUsuarioId(), "ID do usuario");
        validarPeriodo(dataInicial, dataFinal);

        List<ResumoContaDashboard> contas = normalizarLista(dashboardDAO.listarSaldosPorConta(usuarioId));
        BigDecimal saldoTotal = calcularSaldoTotal(contas);
        int contasAtivas = contarContasAtivas(contas);

        BigDecimal totalReceitas = normalizarValor(
                dashboardDAO.calcularReceitasPeriodo(usuarioId, dataInicial, dataFinal)
        );
        BigDecimal totalDespesas = normalizarValor(
                dashboardDAO.calcularDespesasPeriodo(usuarioId, dataInicial, dataFinal)
        );
        BigDecimal resultadoPeriodo = totalReceitas.subtract(totalDespesas);

        List<ResumoCategoriaDashboard> categorias = calcularPercentuaisCategorias(
                normalizarLista(
                        dashboardDAO.listarDespesasPorCategoria(
                                usuarioId,
                                dataInicial,
                                dataFinal,
                                LIMITE_CATEGORIAS
                        )
                ),
                totalDespesas
        );
        List<TransacaoRecenteDashboard> transacoesRecentes = normalizarLista(
                dashboardDAO.listarTransacoesRecentes(
                        usuarioId,
                        dataInicial,
                        dataFinal,
                        LIMITE_TRANSACOES_RECENTES
                )
        );
        List<ResumoCofrinhoDashboard> cofrinhos = calcularPercentuaisCofrinhos(
                normalizarLista(dashboardDAO.listarResumoCofrinhos(usuarioId, LIMITE_COFRINHOS))
        );
        int transacoesPendentes = dashboardDAO.contarTransacoesPendentes(usuarioId);

        return new DashboardResumo(
                dataInicial,
                dataFinal,
                saldoTotal,
                totalReceitas,
                totalDespesas,
                resultadoPeriodo,
                contasAtivas,
                transacoesPendentes,
                contas,
                categorias,
                transacoesRecentes,
                cofrinhos
        );
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

    private Long requireId(Long valor, String nomeCampo) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }
        if (valor <= 0) {
            throw new ValidacaoException(nomeCampo + " deve ser maior que zero.");
        }
        return valor;
    }

    private BigDecimal calcularSaldoTotal(List<ResumoContaDashboard> contas) {
        BigDecimal saldoTotal = BigDecimal.ZERO;
        for (ResumoContaDashboard conta : contas) {
            saldoTotal = saldoTotal.add(normalizarValor(conta.saldoAtual()));
        }
        return saldoTotal;
    }

    private int contarContasAtivas(List<ResumoContaDashboard> contas) {
        int total = 0;
        for (ResumoContaDashboard conta : contas) {
            if (conta.ativa()) {
                total++;
            }
        }
        return total;
    }

    private List<ResumoCategoriaDashboard> calcularPercentuaisCategorias(List<ResumoCategoriaDashboard> categorias,
                                                                         BigDecimal totalDespesas) {
        List<ResumoCategoriaDashboard> resultado = new ArrayList<>();
        BigDecimal denominador = normalizarValor(totalDespesas);

        for (ResumoCategoriaDashboard categoria : categorias) {
            BigDecimal percentual = BigDecimal.ZERO;
            if (denominador.compareTo(BigDecimal.ZERO) > 0) {
                percentual = normalizarValor(categoria.total())
                        .multiply(CEM)
                        .divide(denominador, 2, RoundingMode.HALF_UP);
            }
            resultado.add(new ResumoCategoriaDashboard(
                    categoria.categoriaId(),
                    categoria.nomeCategoria(),
                    categoria.total(),
                    percentual
            ));
        }

        return resultado;
    }

    private List<ResumoCofrinhoDashboard> calcularPercentuaisCofrinhos(List<ResumoCofrinhoDashboard> cofrinhos) {
        List<ResumoCofrinhoDashboard> resultado = new ArrayList<>();

        for (ResumoCofrinhoDashboard cofrinho : cofrinhos) {
            BigDecimal percentual = BigDecimal.ZERO;
            if (cofrinho.valorMeta() != null && cofrinho.valorMeta().compareTo(BigDecimal.ZERO) > 0) {
                percentual = CofrinhoProgressCalculator.calculateProgressPercentage(
                        normalizarValor(cofrinho.valorAtual()),
                        cofrinho.valorMeta()
                );
            }

            resultado.add(new ResumoCofrinhoDashboard(
                    cofrinho.cofrinhoId(),
                    cofrinho.nome(),
                    cofrinho.status(),
                    cofrinho.valorAtual(),
                    cofrinho.valorMeta(),
                    percentual,
                    cofrinho.dataLimite()
            ));
        }

        return resultado;
    }

    private BigDecimal normalizarValor(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private <T> List<T> normalizarLista(List<T> lista) {
        return lista != null ? List.copyOf(lista) : List.of();
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private LocalDate primeiroDiaMesAtual() {
        return hoje().withDayOfMonth(1);
    }
}
