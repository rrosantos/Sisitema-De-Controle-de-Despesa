package br.com.controledespesas.service;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.session.SessaoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

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
    void shouldAuthenticateValidUserAndStartSessionWithoutHash() throws SQLException {
        Usuario usuario = usuarioAtivo();
        when(usuarioDAO.buscarPorEmail("raissa@example.com")).thenReturn(Optional.of(usuario));
        when(passwordHasher.verificar("Senha123", "hash-seguro")).thenReturn(true);

        Usuario autenticado = autenticacaoService.autenticar("RAISSA@EXAMPLE.COM", "Senha123");

        assertTrue(autenticacaoService.estaAutenticado());
        assertEquals(1L, autenticado.getId());
        assertNull(autenticado.getSenhaHash());
        assertNull(sessaoUsuario.getUsuarioAtual().orElseThrow().getSenhaHash());
    }

    @Test
    void shouldReturnGenericMessageWhenEmailDoesNotExist() throws SQLException {
        when(usuarioDAO.buscarPorEmail("raissa@example.com")).thenReturn(Optional.empty());

        AutenticacaoException exception = assertThrows(AutenticacaoException.class,
                () -> autenticacaoService.autenticar("raissa@example.com", "Senha123"));

        assertEquals("E-mail ou senha invalidos.", exception.getMessage());
    }

    @Test
    void shouldReturnGenericMessageWhenPasswordIsIncorrect() throws SQLException {
        Usuario usuario = usuarioAtivo();
        when(usuarioDAO.buscarPorEmail("raissa@example.com")).thenReturn(Optional.of(usuario));
        when(passwordHasher.verificar("SenhaErrada", "hash-seguro")).thenReturn(false);

        AutenticacaoException exception = assertThrows(AutenticacaoException.class,
                () -> autenticacaoService.autenticar("raissa@example.com", "SenhaErrada"));

        assertEquals("E-mail ou senha invalidos.", exception.getMessage());
    }

    @Test
    void shouldRejectInactiveUserWithGenericMessage() throws SQLException {
        Usuario usuario = usuarioAtivo();
        usuario.setAtivo(false);
        when(usuarioDAO.buscarPorEmail("raissa@example.com")).thenReturn(Optional.of(usuario));

        AutenticacaoException exception = assertThrows(AutenticacaoException.class,
                () -> autenticacaoService.autenticar("raissa@example.com", "Senha123"));

        assertEquals("E-mail ou senha invalidos.", exception.getMessage());
    }

    @Test
    void shouldEndSessionOnLogout() throws SQLException {
        Usuario usuario = usuarioAtivo();
        when(usuarioDAO.buscarPorEmail("raissa@example.com")).thenReturn(Optional.of(usuario));
        when(passwordHasher.verificar("Senha123", "hash-seguro")).thenReturn(true);

        autenticacaoService.autenticar("raissa@example.com", "Senha123");
        autenticacaoService.sair();

        assertFalse(autenticacaoService.estaAutenticado());
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setSenhaHash("hash-seguro");
        usuario.setAtivo(true);
        return usuario;
    }
}
