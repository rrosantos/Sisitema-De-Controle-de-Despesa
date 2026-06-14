package br.com.controledespesas.controller;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.MainView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private MainView mainView;

    @Mock
    private ApplicationController applicationController;

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private PasswordHasher passwordHasher;

    private SessaoUsuario sessaoUsuario;
    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        autenticacaoService = new AutenticacaoService(usuarioDAO, passwordHasher, sessaoUsuario);
    }

    @Test
    void shouldDisplayAuthenticatedUser() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(autenticacaoService, sessaoUsuario, mainView, applicationController);

        mainController.iniciar();

        verify(mainView).exibirUsuario("Raissa", "raissa@example.com");
        verify(mainView).definirAcaoSair(any(Runnable.class));
        verify(mainView).abrir();
    }

    @Test
    void shouldLogoutAndReturnToLogin() {
        sessaoUsuario.iniciar(usuario());
        MainController mainController =
                new MainController(autenticacaoService, sessaoUsuario, mainView, applicationController);

        mainController.realizarLogout();

        assertFalse(sessaoUsuario.estaAutenticado());
        verify(mainView).fechar();
        verify(applicationController).realizarLogout();
    }

    @Test
    void shouldRedirectToLoginWhenThereIsNoSession() {
        MainController mainController =
                new MainController(autenticacaoService, sessaoUsuario, mainView, applicationController);

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
}
