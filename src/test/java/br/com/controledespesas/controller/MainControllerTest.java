package br.com.controledespesas.controller;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.InicioPanel;
import br.com.controledespesas.view.contract.MainView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private MainView mainView;

    @Mock
    private ApplicationController applicationController;

    @Mock
    private TransacaoController transacaoController;

    @Mock
    private CategoriaController categoriaController;

    @Mock
    private ContaController contaController;

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private PasswordHasher passwordHasher;

    private SessaoUsuario sessaoUsuario;
    private AutenticacaoService autenticacaoService;
    private InicioPanel inicioPanel;
    private JPanel transacaoPanel;
    private JPanel categoriaPanel;
    private JPanel contaPanel;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        autenticacaoService = new AutenticacaoService(usuarioDAO, passwordHasher, sessaoUsuario);
        inicioPanel = new InicioPanel();
        transacaoPanel = new JPanel();
        categoriaPanel = new JPanel();
        contaPanel = new JPanel();
    }

    @Test
    void shouldDisplayAuthenticatedUserAndInitializePanels() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();

        verify(mainView).exibirUsuario("Raissa", "raissa@example.com");
        verify(mainView).adicionarPainel(eq(MainController.PAINEL_INICIO), eq(inicioPanel));
        verify(mainView).adicionarPainel(eq(MainController.PAINEL_TRANSACOES), eq(transacaoPanel));
        verify(mainView).adicionarPainel(eq(MainController.PAINEL_CATEGORIAS), eq(categoriaPanel));
        verify(mainView).adicionarPainel(eq(MainController.PAINEL_CONTAS), eq(contaPanel));
        verify(mainView).definirAcaoInicio(any(Runnable.class));
        verify(mainView).definirAcaoTransacoes(any(Runnable.class));
        verify(mainView).definirAcaoCategorias(any(Runnable.class));
        verify(mainView).definirAcaoContas(any(Runnable.class));
        verify(mainView).definirAcaoSair(any(Runnable.class));
        verify(mainView).mostrarPainel(MainController.PAINEL_INICIO);
        verify(mainView).definirMenuAtivo(MainController.PAINEL_INICIO);
        verify(mainView).abrir();
        verify(transacaoController, never()).carregar();
        verify(categoriaController, never()).carregar();
        verify(contaController, never()).carregar();
    }

    @Test
    void shouldNavigateToTransactions() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();
        clearInvocations(mainView, transacaoController, categoriaController, contaController);

        mainController.mostrarTransacoes();

        verify(mainView).mostrarPainel(MainController.PAINEL_TRANSACOES);
        verify(mainView).definirMenuAtivo(MainController.PAINEL_TRANSACOES);
        verify(transacaoController).carregar();
        verify(categoriaController, never()).carregar();
        verify(contaController, never()).carregar();
    }

    @Test
    void shouldNavigateToCategoriesWithoutRegisteringPanelsAgain() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();
        clearInvocations(mainView, categoriaController, contaController);

        mainController.mostrarCategorias();
        mainController.mostrarCategorias();

        verify(mainView, times(2)).mostrarPainel(MainController.PAINEL_CATEGORIAS);
        verify(mainView, times(2)).definirMenuAtivo(MainController.PAINEL_CATEGORIAS);
        verify(categoriaController, times(2)).carregar();
        verify(mainView, never()).adicionarPainel(any(), any());
        verify(transacaoController, never()).carregar();
        verify(contaController, never()).carregar();
    }

    @Test
    void shouldNavigateToAccounts() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();
        clearInvocations(mainView, categoriaController, contaController);

        mainController.mostrarContas();

        verify(mainView).mostrarPainel(MainController.PAINEL_CONTAS);
        verify(mainView).definirMenuAtivo(MainController.PAINEL_CONTAS);
        verify(contaController).carregar();
        verify(categoriaController, never()).carregar();
    }

    @Test
    void shouldNavigateToTransactionsFromInicioCard() throws Exception {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();
        clearInvocations(mainView, transacaoController, categoriaController, contaController);

        obterBotaoTransacoes(inicioPanel).doClick();

        verify(mainView).mostrarPainel(MainController.PAINEL_TRANSACOES);
        verify(mainView).definirMenuAtivo(MainController.PAINEL_TRANSACOES);
        verify(transacaoController).carregar();
    }

    @Test
    void shouldLogoutAndReturnToLogin() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.realizarLogout();

        assertFalse(sessaoUsuario.estaAutenticado());
        verify(mainView).fechar();
        verify(applicationController).realizarLogout();
    }

    @Test
    void shouldRedirectToLoginWhenThereIsNoSession() {
        MainController mainController =
                new MainController(
                        autenticacaoService,
                        sessaoUsuario,
                        mainView,
                        applicationController,
                        inicioPanel,
                        transacaoPanel,
                        categoriaPanel,
                        contaPanel,
                        transacaoController,
                        categoriaController,
                        contaController
                );

        mainController.iniciar();

        verify(mainView).fechar();
        verify(applicationController).mostrarLogin();
        verify(mainView, never()).abrir();
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private JButton obterBotaoTransacoes(InicioPanel painel) throws Exception {
        Field field = InicioPanel.class.getDeclaredField("transacoesButton");
        field.setAccessible(true);
        return (JButton) field.get(painel);
    }
}
