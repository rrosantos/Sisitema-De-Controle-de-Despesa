package br.com.controledespesas.service;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private PasswordHasher passwordHasher;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioDAO, passwordHasher);
    }

    @Test
    void shouldRegisterValidUserWithHashedPasswordAndReturnWithoutHash() throws SQLException {
        when(usuarioDAO.emailExiste("raissa@example.com")).thenReturn(false);
        when(passwordHasher.gerarHash("Senha123")).thenReturn("hash-gerado");
        when(usuarioDAO.inserir(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(1L);
            return 1L;
        });

        Usuario usuario = usuarioService.cadastrar("  Raissa  ", "RAISSA@EXAMPLE.COM", "Senha123", "Senha123");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDAO).inserir(captor.capture());

        assertEquals("hash-gerado", captor.getValue().getSenhaHash());
        assertTrue(captor.getValue().isAtivo());
        assertEquals("Raissa", usuario.getNome());
        assertEquals("raissa@example.com", usuario.getEmail());
        assertNull(usuario.getSenhaHash());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(ValidacaoException.class,
                () -> usuarioService.cadastrar("   ", "raissa@example.com", "Senha123", "Senha123"));
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(ValidacaoException.class,
                () -> usuarioService.cadastrar("Raissa", "email-invalido", "Senha123", "Senha123"));
    }

    @Test
    void shouldRejectDuplicateEmail() throws SQLException {
        when(usuarioDAO.emailExiste("raissa@example.com")).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.cadastrar("Raissa", "raissa@example.com", "Senha123", "Senha123"));
    }

    @Test
    void shouldRejectPasswordConfirmationMismatch() {
        assertThrows(ValidacaoException.class,
                () -> usuarioService.cadastrar("Raissa", "raissa@example.com", "Senha123", "Senha124"));
    }

    @Test
    void shouldRejectShortPassword() {
        assertThrows(ValidacaoException.class,
                () -> usuarioService.cadastrar("Raissa", "raissa@example.com", "1234567", "1234567"));
    }

    @Test
    void shouldRejectUpdatingUserWithAnotherUsersEmail() throws SQLException {
        when(usuarioDAO.buscarPorId(1L)).thenReturn(Optional.of(usuarioExistente()));
        when(usuarioDAO.emailExisteParaOutroUsuario("outro@example.com", 1L)).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> usuarioService.atualizarDados(1L, "Raissa", "outro@example.com"));
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect() throws SQLException {
        when(usuarioDAO.buscarPorId(1L)).thenReturn(Optional.of(usuarioExistente()));
        when(passwordHasher.verificar("SenhaAtualErrada", "hash-atual")).thenReturn(false);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> usuarioService.alterarSenha(1L, "SenhaAtualErrada", "NovaSenha123", "NovaSenha123"));

        assertEquals("A senha atual informada esta incorreta.", exception.getMessage());
        verify(usuarioDAO, never()).atualizarSenha(any(Long.class), any(String.class));
    }

    private Usuario usuarioExistente() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setSenhaHash("hash-atual");
        usuario.setAtivo(true);
        return usuario;
    }
}
