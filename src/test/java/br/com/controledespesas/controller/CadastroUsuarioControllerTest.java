package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.UsuarioService;
import br.com.controledespesas.view.contract.CadastroUsuarioView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroUsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private CadastroUsuarioView cadastroUsuarioView;

    @Mock
    private ApplicationController applicationController;

    private CadastroUsuarioController cadastroUsuarioController;

    @BeforeEach
    void setUp() {
        cadastroUsuarioController = new CadastroUsuarioController(
                usuarioService,
                cadastroUsuarioView,
                applicationController,
                new ImmediateAsyncTaskExecutor()
        );
    }

    @Test
    void shouldRegisterUserAndReturnToLogin() throws Exception {
        char[] senha = "Senha123".toCharArray();
        char[] confirmacao = "Senha123".toCharArray();
        when(cadastroUsuarioView.getNome()).thenReturn("Raissa");
        when(cadastroUsuarioView.getEmail()).thenReturn("raissa@example.com");
        when(cadastroUsuarioView.getSenha()).thenReturn(senha);
        when(cadastroUsuarioView.getConfirmacaoSenha()).thenReturn(confirmacao);
        when(usuarioService.cadastrar("Raissa", "raissa@example.com", "Senha123", "Senha123"))
                .thenReturn(usuario());

        cadastroUsuarioController.cadastrar();

        assertArrayEquals(new char[senha.length], senha);
        assertArrayEquals(new char[confirmacao.length], confirmacao);
        verify(cadastroUsuarioView).limparCampos();
        verify(cadastroUsuarioView).limparSenhas();
        verify(applicationController)
                .mostrarLoginComEmail("raissa@example.com", "Conta criada com sucesso. Agora voce ja pode entrar.");
    }

    @Test
    void shouldShowValidationMessage() throws Exception {
        when(cadastroUsuarioView.getNome()).thenReturn("");
        when(cadastroUsuarioView.getEmail()).thenReturn("raissa@example.com");
        when(cadastroUsuarioView.getSenha()).thenReturn("Senha123".toCharArray());
        when(cadastroUsuarioView.getConfirmacaoSenha()).thenReturn("Senha123".toCharArray());
        when(usuarioService.cadastrar("", "raissa@example.com", "Senha123", "Senha123"))
                .thenThrow(new ValidacaoException("Nome e obrigatorio."));

        cadastroUsuarioController.cadastrar();

        verify(cadastroUsuarioView).mostrarErro("Nome e obrigatorio.");
        verify(cadastroUsuarioView).limparSenhas();
    }

    @Test
    void shouldShowBusinessRuleMessage() throws Exception {
        when(cadastroUsuarioView.getNome()).thenReturn("Raissa");
        when(cadastroUsuarioView.getEmail()).thenReturn("raissa@example.com");
        when(cadastroUsuarioView.getSenha()).thenReturn("Senha123".toCharArray());
        when(cadastroUsuarioView.getConfirmacaoSenha()).thenReturn("Senha123".toCharArray());
        when(usuarioService.cadastrar("Raissa", "raissa@example.com", "Senha123", "Senha123"))
                .thenThrow(new RegraNegocioException("Ja existe um usuario cadastrado com este e-mail."));

        cadastroUsuarioController.cadastrar();

        verify(cadastroUsuarioView).mostrarErro("Ja existe um usuario cadastrado com este e-mail.");
        verify(cadastroUsuarioView).limparSenhas();
    }

    @Test
    void shouldShowGenericDatabaseMessageOnSqlError() throws Exception {
        when(cadastroUsuarioView.getNome()).thenReturn("Raissa");
        when(cadastroUsuarioView.getEmail()).thenReturn("raissa@example.com");
        when(cadastroUsuarioView.getSenha()).thenReturn("Senha123".toCharArray());
        when(cadastroUsuarioView.getConfirmacaoSenha()).thenReturn("Senha123".toCharArray());
        when(usuarioService.cadastrar("Raissa", "raissa@example.com", "Senha123", "Senha123"))
                .thenThrow(new SQLException("db off"));

        cadastroUsuarioController.cadastrar();

        verify(cadastroUsuarioView).mostrarErro("Nao foi possivel acessar o banco de dados. Tente novamente.");
        verify(cadastroUsuarioView).limparSenhas();
    }

    @Test
    void shouldReturnToLoginOnBackAction() {
        cadastroUsuarioController.voltarParaLogin();

        verify(cadastroUsuarioView).limparMensagem();
        verify(applicationController).mostrarLogin();
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
