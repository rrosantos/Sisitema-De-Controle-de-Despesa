package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.DashboardDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.model.Usuario;
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

    public MainController(SessaoUsuario sessaoUsuario, TransacaoDAO transacaoDAO,
                          CategoriaDAO categoriaDAO, ContaDAO contaDAO, DashboardDAO dashboardDAO,
                          ConnectionProvider connectionProvider,
                          CofrinhoDAO cofrinhoDAO, MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO,
                          AsyncTaskExecutor asyncTaskExecutor, MainView mainView,
                          ApplicationController applicationController) {
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        Objects.requireNonNull(transacaoDAO, "transacaoDAO nao pode ser nulo.");
        Objects.requireNonNull(categoriaDAO, "categoriaDAO nao pode ser nulo.");
        Objects.requireNonNull(contaDAO, "contaDAO nao pode ser nulo.");
        Objects.requireNonNull(dashboardDAO, "dashboardDAO nao pode ser nulo.");
        Objects.requireNonNull(connectionProvider, "connectionProvider nao pode ser nulo.");
        Objects.requireNonNull(cofrinhoDAO, "cofrinhoDAO nao pode ser nulo.");
        Objects.requireNonNull(movimentacaoCofrinhoDAO, "movimentacaoCofrinhoDAO nao pode ser nulo.");
        Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");

        this.inicioPanel = new InicioPanel();
        this.dashboardController = new DashboardController(
                dashboardDAO,
                sessaoUsuario,
                inicioPanel,
                asyncTaskExecutor
        );

        TransacaoPanel transacaoPanelConcreto = new TransacaoPanel();
        this.transacaoPanel = transacaoPanelConcreto;
        this.transacaoController = new TransacaoController(
                transacaoDAO,
                categoriaDAO,
                contaDAO,
                connectionProvider,
                sessaoUsuario,
                transacaoPanelConcreto,
                asyncTaskExecutor,
                dashboardController
        );

        CategoriaPanel categoriaPanelConcreto = new CategoriaPanel();
        this.categoriaPanel = categoriaPanelConcreto;
        this.categoriaController =
                new CategoriaController(categoriaDAO, sessaoUsuario, categoriaPanelConcreto, asyncTaskExecutor);

        ContaPanel contaPanelConcreto = new ContaPanel();
        this.contaPanel = contaPanelConcreto;
        this.contaController =
                new ContaController(contaDAO, sessaoUsuario, contaPanelConcreto, asyncTaskExecutor, dashboardController);

        CofrinhoPanel cofrinhoPanelConcreto = new CofrinhoPanel();
        this.cofrinhoPanel = cofrinhoPanelConcreto;
        this.cofrinhoController = new CofrinhoController(
                cofrinhoDAO,
                movimentacaoCofrinhoDAO,
                connectionProvider,
                sessaoUsuario,
                cofrinhoPanelConcreto,
                asyncTaskExecutor,
                dashboardController
        );
    }

    MainController(SessaoUsuario sessaoUsuario, MainView mainView, ApplicationController applicationController,
                   InicioPanel inicioPanel, JPanel transacaoPanel, JPanel categoriaPanel, JPanel contaPanel,
                   JPanel cofrinhoPanel,
                   DashboardController dashboardController,
                   TransacaoController transacaoController, CategoriaController categoriaController,
                   ContaController contaController, CofrinhoController cofrinhoController) {
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
        sessaoUsuario.encerrar();
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
