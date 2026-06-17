package br.com.controledespesas.session;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;

import java.util.Objects;
import java.util.Optional;

public class SessaoUsuario {

    private Usuario usuarioAtual;

    public void iniciar(Usuario usuario) {
        Objects.requireNonNull(usuario, "O usuario autenticado nao pode ser nulo.");
        Objects.requireNonNull(usuario.getId(), "O usuario autenticado precisa possuir ID.");

        if (!usuario.isAtivo()) {
            throw new AutenticacaoException("Nao e permitido iniciar sessao com usuario inativo.");
        }

        this.usuarioAtual = sanitizarUsuario(usuario);
    }

    public void encerrar() {
        this.usuarioAtual = null;
    }

    public boolean estaAutenticado() {
        return usuarioAtual != null;
    }

    public Optional<Usuario> getUsuarioAtual() {
        return Optional.ofNullable(usuarioAtual).map(this::sanitizarUsuario);
    }

    public Usuario exigirUsuarioAutenticado() {
        if (usuarioAtual == null) {
            throw new AutenticacaoException("Nenhum usuario autenticado na sessao.");
        }

        return sanitizarUsuario(usuarioAtual);
    }

    public Long exigirUsuarioId() {
        return exigirUsuarioAutenticado().getId();
    }

    private Usuario sanitizarUsuario(Usuario usuario) {
        Usuario copia = new Usuario();
        copia.setId(usuario.getId());
        copia.setNome(usuario.getNome());
        copia.setEmail(usuario.getEmail());
        copia.setAtivo(usuario.isAtivo());
        copia.setCriadoEm(usuario.getCriadoEm());
        copia.setAtualizadoEm(usuario.getAtualizadoEm());
        copia.setSenhaHash(null);
        return copia;
    }
}
