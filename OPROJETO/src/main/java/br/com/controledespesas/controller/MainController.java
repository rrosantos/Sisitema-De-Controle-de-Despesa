package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.service.CofrinhoService;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.service.DashboardService;
import br.com.controledespesas.service.MovimentacaoCofrinhoService;
import br.com.controledespesas.service.TransacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.CofrinhoPanel;
import br.com.controledespesas.view.CategoriaPanel;
import br.com.controledespesas.view.ContaPanel;
import br.com.controledespesas.view.InicioPanel;
import br.com.controledespesas.view.TransacaoPanel;
import br.com.controledespesas.view.contract.MainView;

import javax.swing.JPanel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    static final String PAINEL_INICIO = "inicio";
    static final String PAINEL_TRANSACOES = "transacoes";
    static final String PAINEL_CATEGORIAS = "categorias";
    static final String PAINEL_CONTAS = "contas";
    static final String PAINEL_COFRINHOS = "cofrinhos";

    private final AutenticacaoService autenticacaoService;
    private final SessaoUsuario sessaoUsuario;
    private final MainView mainView;
    private final ApplicationController applicationController;
    private final InicioPanel inicioPanel;
    private final JPanel transacaoPanel;
    private final JPanel categoriaPanel;
    private final JPanel contaPanel;
    private final JPanel cofrinhoPanel;
    private final DashboardController dashboardController;
    private final TransacaoController transacaoController;
    private final CategoriaController categoriaController;
    private final ContaController contaController;
    private final CofrinhoController cofrinhoController;

    private boolean componentesRegistrados;

    public MainController(AutenticacaoService autenticacaoService, SessaoUsuario sessaoUsuario,
                          TransacaoService transacaoService, CategoriaService categoriaService, ContaService contaService,
                          CofrinhoService cofrinhoService, MovimentacaoCofrinhoService movimentacaoCofrinhoService,
                          DashboardService dashboardService,
                          AsyncTaskExecutor asyncTaskExecutor, MainView mainView,
                          ApplicationController applicationController) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        Objects.requireNonNull(transacaoService, "transacaoService nao pode ser nulo.");
        Objects.requireNonNull(categoriaService, "categoriaService nao pode ser nulo.");
        Objects.requireNonNull(contaService, "contaService nao pode ser nulo.");
        Objects.requireNonNull(cofrinhoService, "cofrinhoService nao pode ser nulo.");
        Objects.requireNonNull(movimentacaoCofrinhoService, "movimentacaoCofrinhoService nao pode ser nulo.");
        Objects.requireNonNull(dashboardService, "dashboardService nao pode ser nulo.");
        Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");

        this.inicioPanel = new InicioPanel();
        this.dashboardController = new DashboardController(
                dashboardService,
                sessaoUsuario,
                inicioPanel,
                asyncTaskExecutor
        );

        TransacaoPanel transacaoPanelConcreto = new TransacaoPanel();
        this.transacaoPanel = transacaoPanelConcreto;
        this.transacaoController = new TransacaoController(
                transacaoService,
                categoriaService,
                contaService,
                sessaoUsuario,
                transacaoPanelConcreto,
                asyncTaskExecutor,
                dashboardController
        );

        CategoriaPanel categoriaPanelConcreto = new CategoriaPanel();
        this.categoriaPanel = categoriaPanelConcreto;
        this.categoriaController =
                new CategoriaController(categoriaService, sessaoUsuario, categoriaPanelConcreto, asyncTaskExecutor);

        ContaPanel contaPanelConcreto = new ContaPanel();
        this.contaPanel = contaPanelConcreto;
        this.contaController =
                new ContaController(contaService, sessaoUsuario, contaPanelConcreto, asyncTaskExecutor, dashboardController);

        CofrinhoPanel cofrinhoPanelConcreto = new CofrinhoPanel();
        this.cofrinhoPanel = cofrinhoPanelConcreto;
        this.cofrinhoController = new CofrinhoController(
                cofrinhoService,
                movimentacaoCofrinhoService,
                sessaoUsuario,
                cofrinhoPanelConcreto,
                asyncTaskExecutor,
                dashboardController
        );
    }

    MainController(AutenticacaoService autenticacaoService, SessaoUsuario sessaoUsuario,
                   MainView mainView, ApplicationController applicationController,
                   InicioPanel inicioPanel, JPanel transacaoPanel, JPanel categoriaPanel, JPanel contaPanel,
                   JPanel cofrinhoPanel,
                   DashboardController dashboardController,
                   TransacaoController transacaoController, CategoriaController categoriaController,
                   ContaController contaController, CofrinhoController cofrinhoController) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        this.inicioPanel = Objects.requireNonNull(inicioPanel, "inicioPanel nao pode ser nulo.");
        this.transacaoPanel = Objects.requireNonNull(transacaoPanel, "transacaoPanel nao pode ser nulo.");
        this.categoriaPanel = Objects.requireNonNull(categoriaPanel, "categoriaPanel nao pode ser nulo.");
        this.contaPanel = Objects.requireNonNull(contaPanel, "contaPanel nao pode ser nulo.");
        this.cofrinhoPanel = Objects.requireNonNull(cofrinhoPanel, "cofrinhoPanel nao pode ser nulo.");
        this.dashboardController = Objects.requireNonNull(dashboardController, "dashboardController nao pode ser nulo.");
        this.transacaoController = Objects.requireNonNull(
                transacaoController,
                "transacaoController nao pode ser nulo."
        );
        this.categoriaController = Objects.requireNonNull(categoriaController, "categoriaController nao pode ser nulo.");
        this.contaController = Objects.requireNonNull(contaController, "contaController nao pode ser nulo.");
        this.cofrinhoController = Objects.requireNonNull(cofrinhoController, "cofrinhoController nao pode ser nulo.");
    }

    public void iniciar() {
        try {
            Usuario usuario = sessaoUsuario.exigirUsuarioAutenticado();
            configurarTela(usuario);
            mostrarInicio();
            mainView.abrir();
        } catch (AutenticacaoException exception) {
            tratarSessaoInvalida(exception);
        }
    }

    public void mostrarInicio() {
        Usuario usuario = exigirUsuarioAtual();
        if (usuario == null) {
            return;
        }

        inicioPanel.exibirUsuario(usuario.getNome());
        mainView.mostrarPainel(PAINEL_INICIO);
        mainView.definirMenuAtivo(PAINEL_INICIO);
        dashboardController.atualizarSeNecessario();
    }

    public void mostrarCategorias() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_CATEGORIAS);
        mainView.definirMenuAtivo(PAINEL_CATEGORIAS);
        categoriaController.carregar();
    }

    public void mostrarTransacoes() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_TRANSACOES);
        mainView.definirMenuAtivo(PAINEL_TRANSACOES);
        transacaoController.carregar();
    }

    public void mostrarContas() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_CONTAS);
        mainView.definirMenuAtivo(PAINEL_CONTAS);
        contaController.carregar();
    }

    public void mostrarCofrinhos() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_COFRINHOS);
        mainView.definirMenuAtivo(PAINEL_COFRINHOS);
        cofrinhoController.carregar();
    }

    public void realizarLogout() {
        autenticacaoService.sair();
        mainView.fechar();
        applicationController.realizarLogout();
    }

    private void configurarTela(Usuario usuario) {
        mainView.exibirUsuario(usuario.getNome(), usuario.getEmail());
        inicioPanel.exibirUsuario(usuario.getNome());

        if (componentesRegistrados) {
            return;
        }

        mainView.adicionarPainel(PAINEL_INICIO, inicioPanel);
        mainView.adicionarPainel(PAINEL_TRANSACOES, transacaoPanel);
        mainView.adicionarPainel(PAINEL_CATEGORIAS, categoriaPanel);
        mainView.adicionarPainel(PAINEL_CONTAS, contaPanel);
        mainView.adicionarPainel(PAINEL_COFRINHOS, cofrinhoPanel);
        mainView.definirAcaoInicio(this::mostrarInicio);
        mainView.definirAcaoTransacoes(this::mostrarTransacoes);
        mainView.definirAcaoCategorias(this::mostrarCategorias);
        mainView.definirAcaoContas(this::mostrarContas);
        mainView.definirAcaoCofrinhos(this::mostrarCofrinhos);
        mainView.definirAcaoSair(this::realizarLogout);
        dashboardController.definirAcaoAbrirTransacoes(this::mostrarTransacoes);
        dashboardController.definirAcaoAbrirContas(this::mostrarContas);
        dashboardController.definirAcaoAbrirCofrinhos(this::mostrarCofrinhos);
        inicioPanel.definirAcaoTransacoes(dashboardController::abrirTransacoes);
        inicioPanel.definirAcaoCategorias(this::mostrarCategorias);
        inicioPanel.definirAcaoContas(dashboardController::abrirContas);
        inicioPanel.definirAcaoCofrinhos(dashboardController::abrirCofrinhos);
        componentesRegistrados = true;
    }

    private Usuario exigirUsuarioAtual() {
        try {
            return sessaoUsuario.exigirUsuarioAutenticado();
        } catch (AutenticacaoException exception) {
            tratarSessaoInvalida(exception);
            return null;
        }
    }

    private void tratarSessaoInvalida(AutenticacaoException exception) {
        LOGGER.log(Level.FINE, "Tentativa de abrir tela principal sem sessao autenticada.", exception);
        mainView.fechar();
        applicationController.mostrarLogin();
    }
}
