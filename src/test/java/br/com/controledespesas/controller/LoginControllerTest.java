package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.view.contract.LoginView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private AutenticacaoService autenticacaoService;

    @Mock
    private LoginView loginView;

    @Mock
    private ApplicationController applicationController;

    private LoginController loginController;

    @BeforeEach
    void setUp() {
        loginController = new LoginController(
                autenticacaoService,
                loginView,
                applicationController,
                new ImmediateAsyncTaskExecutor()
        );
    }

    @Test
    void shouldOpenMainScreenAfterSuccessfulLogin() throws Exception {
        char[] senha = "Senha123".toCharArray();
        when(loginView.getEmail()).thenReturn("raissa@example.com");
        when(loginView.getSenha()).thenReturn(senha);
        when(autenticacaoService.autenticar("raissa@example.com", "Senha123")).thenReturn(usuario());

        loginController.entrar();

        assertArrayEquals(new char[senha.length], senha);
        verify(applicationController).mostrarTelaPrincipal();
        verify(loginView).setCarregando(true);
        verify(loginView).setCarregando(false);
        verify(loginView).limparSenha();
    }

    @Test
    void shouldShowGenericMessageWhenAuthenticationFails() throws Exception {
        when(loginView.getEmail()).thenReturn("raissa@example.com");
        when(loginView.getSenha()).thenReturn("SenhaErrada".toCharArray());
        when(autenticacaoService.autenticar("raissa@example.com", "SenhaErrada"))
                .thenThrow(new AutenticacaoException("E-mail ou senha invalidos."));

        loginController.entrar();

        verify(loginView).mostrarErro("E-mail ou senha invalidos.");
        verify(loginView).setCarregando(false);
        verify(loginView).limparSenha();
        verify(applicationController, never()).mostrarTelaPrincipal();
    }

    @Test
    void shouldShowGenericDatabaseMessageOnSqlError() throws Exception {
        when(loginView.getEmail()).thenReturn("raissa@example.com");
        when(loginView.getSenha()).thenReturn("Senha123".toCharArray());
        when(autenticacaoService.autenticar("raissa@example.com", "Senha123"))
                .thenThrow(new SQLException("db off"));

        loginController.entrar();

        verify(loginView).mostrarErro("Nao foi possivel acessar o banco de dados. Tente novamente.");
        verify(loginView).setCarregando(false);
        verify(loginView).limparSenha();
    }

    @Test
    void shouldCleanPasswordAfterAttempt() throws Exception {
        char[] senha = "Senha123".toCharArray();
        when(loginView.getEmail()).thenReturn("raissa@example.com");
        when(loginView.getSenha()).thenReturn(senha);
        when(autenticacaoService.autenticar("raissa@example.com", "Senha123"))
                .thenThrow(new AutenticacaoException("E-mail ou senha invalidos."));

        loginController.entrar();

        assertArrayEquals(new char[senha.length], senha);
        verify(loginView).limparSenha();
    }

    @Test
    void shouldNavigateToRegistrationOnCreateAccountAction() {
        loginController.abrirCadastro();

        verify(loginView).limparMensagem();
        verify(applicationController).mostrarCadastro();
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
