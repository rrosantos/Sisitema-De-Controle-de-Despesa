package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.CategoriaPanel;
import br.com.controledespesas.view.ContaPanel;
import br.com.controledespesas.view.InicioPanel;
import br.com.controledespesas.view.contract.MainView;

import javax.swing.JPanel;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    static final String PAINEL_INICIO = "inicio";
    static final String PAINEL_CATEGORIAS = "categorias";
    static final String PAINEL_CONTAS = "contas";

    private final AutenticacaoService autenticacaoService;
    private final SessaoUsuario sessaoUsuario;
    private final MainView mainView;
    private final ApplicationController applicationController;
    private final InicioPanel inicioPanel;
    private final JPanel categoriaPanel;
    private final JPanel contaPanel;
    private final CategoriaController categoriaController;
    private final ContaController contaController;

    private boolean componentesRegistrados;

    public MainController(AutenticacaoService autenticacaoService, SessaoUsuario sessaoUsuario,
                          CategoriaService categoriaService, ContaService contaService,
                          AsyncTaskExecutor asyncTaskExecutor, MainView mainView,
                          ApplicationController applicationController) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        Objects.requireNonNull(categoriaService, "categoriaService nao pode ser nulo.");
        Objects.requireNonNull(contaService, "contaService nao pode ser nulo.");
        Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");

        this.inicioPanel = new InicioPanel();

        CategoriaPanel categoriaPanelConcreto = new CategoriaPanel();
        this.categoriaPanel = categoriaPanelConcreto;
        this.categoriaController =
                new CategoriaController(categoriaService, sessaoUsuario, categoriaPanelConcreto, asyncTaskExecutor);

        ContaPanel contaPanelConcreto = new ContaPanel();
        this.contaPanel = contaPanelConcreto;
        this.contaController =
                new ContaController(contaService, sessaoUsuario, contaPanelConcreto, asyncTaskExecutor);
    }

    MainController(AutenticacaoService autenticacaoService, SessaoUsuario sessaoUsuario,
                   MainView mainView, ApplicationController applicationController,
                   InicioPanel inicioPanel, JPanel categoriaPanel, JPanel contaPanel,
                   CategoriaController categoriaController, ContaController contaController) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        this.inicioPanel = Objects.requireNonNull(inicioPanel, "inicioPanel nao pode ser nulo.");
        this.categoriaPanel = Objects.requireNonNull(categoriaPanel, "categoriaPanel nao pode ser nulo.");
        this.contaPanel = Objects.requireNonNull(contaPanel, "contaPanel nao pode ser nulo.");
        this.categoriaController = Objects.requireNonNull(categoriaController, "categoriaController nao pode ser nulo.");
        this.contaController = Objects.requireNonNull(contaController, "contaController nao pode ser nulo.");
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
    }

    public void mostrarCategorias() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_CATEGORIAS);
        mainView.definirMenuAtivo(PAINEL_CATEGORIAS);
        categoriaController.carregar();
    }

    public void mostrarContas() {
        if (exigirUsuarioAtual() == null) {
            return;
        }

        mainView.mostrarPainel(PAINEL_CONTAS);
        mainView.definirMenuAtivo(PAINEL_CONTAS);
        contaController.carregar();
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
        mainView.adicionarPainel(PAINEL_CATEGORIAS, categoriaPanel);
        mainView.adicionarPainel(PAINEL_CONTAS, contaPanel);
        mainView.definirAcaoInicio(this::mostrarInicio);
        mainView.definirAcaoCategorias(this::mostrarCategorias);
        mainView.definirAcaoContas(this::mostrarContas);
        mainView.definirAcaoSair(this::realizarLogout);
        inicioPanel.definirAcaoCategorias(this::mostrarCategorias);
        inicioPanel.definirAcaoContas(this::mostrarContas);
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
