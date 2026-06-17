package br.com.controledespesas.service;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.session.SessaoUsuario;

import java.sql.SQLException;
import java.util.Objects;

public class AutenticacaoService {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha invalidos.";

    private final UsuarioDAO usuarioDAO;
    private final PasswordHasher passwordHasher;
    private final SessaoUsuario sessaoUsuario;

    public AutenticacaoService() {
        this(new UsuarioDAO(), new PasswordHasher(), new SessaoUsuario());
    }

    public AutenticacaoService(UsuarioDAO usuarioDAO, PasswordHasher passwordHasher, SessaoUsuario sessaoUsuario) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO nao pode ser nulo.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
    }

    public Usuario autenticar(String email, String senha) throws SQLException {
        String emailNormalizado = ServiceValidationUtils.normalizeEmail(email);
        validarSenhaInformada(senha);

        Usuario usuario = usuarioDAO.buscarPorEmail(emailNormalizado)
                .orElseThrow(() -> new AutenticacaoException(MENSAGEM_CREDENCIAIS_INVALIDAS));

        if (!usuario.isAtivo() || !passwordHasher.verificar(senha, usuario.getSenhaHash())) {
            throw new AutenticacaoException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        sessaoUsuario.iniciar(usuario);
        return sessaoUsuario.exigirUsuarioAutenticado();
    }

    public void sair() {
        sessaoUsuario.encerrar();
    }

    public boolean estaAutenticado() {
        return sessaoUsuario.estaAutenticado();
    }

    private void validarSenhaInformada(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new ValidacaoException("A senha e obrigatoria.");
        }
    }
}
