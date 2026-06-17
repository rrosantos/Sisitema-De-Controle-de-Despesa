package br.com.controledespesas.service;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Objects;

public class UsuarioService {

    private static final int MAX_NOME = 150;
    private static final String MENSAGEM_EMAIL_DUPLICADO = "Ja existe um usuario cadastrado com este e-mail.";

    private final UsuarioDAO usuarioDAO;
    private final PasswordHasher passwordHasher;

    public UsuarioService() {
        this(new UsuarioDAO(), new PasswordHasher());
    }

    public UsuarioService(UsuarioDAO usuarioDAO, PasswordHasher passwordHasher) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO nao pode ser nulo.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher nao pode ser nulo.");
    }

    public Usuario cadastrar(String nome, String email, String senha, String confirmacaoSenha) throws SQLException {
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome", MAX_NOME);
        String emailNormalizado = ServiceValidationUtils.normalizeEmail(email);
        ServiceValidationUtils.validatePassword(senha);
        ServiceValidationUtils.validatePasswordConfirmation(senha, confirmacaoSenha);

        if (usuarioDAO.emailExiste(emailNormalizado)) {
            throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO);
        }

        try {
            Usuario usuario = new Usuario();
            usuario.setNome(nomeNormalizado);
            usuario.setEmail(emailNormalizado);
            usuario.setSenhaHash(passwordHasher.gerarHash(senha));
            usuario.setAtivo(true);

            usuarioDAO.inserir(usuario);
            return sanitizarUsuario(usuario);
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO, exception);
            }
            throw exception;
        }
    }

    public Usuario buscarPorId(Long usuarioId) throws SQLException {
        ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return sanitizarUsuario(buscarUsuarioExistente(usuarioId));
    }

    public Usuario atualizarDados(Long usuarioId, String nome, String email) throws SQLException {
        Long id = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome", MAX_NOME);
        String emailNormalizado = ServiceValidationUtils.normalizeEmail(email);

        Usuario usuarioExistente = buscarUsuarioExistente(id);
        if (usuarioDAO.emailExisteParaOutroUsuario(emailNormalizado, id)) {
            throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO);
        }

        if (Objects.equals(usuarioExistente.getNome(), nomeNormalizado)
                && Objects.equals(usuarioExistente.getEmail(), emailNormalizado)) {
            return sanitizarUsuario(usuarioExistente);
        }

        usuarioExistente.setNome(nomeNormalizado);
        usuarioExistente.setEmail(emailNormalizado);

        try {
            usuarioDAO.atualizar(usuarioExistente);
            return sanitizarUsuario(usuarioExistente);
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO, exception);
            }
            throw exception;
        }
    }

    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha, String confirmacaoNovaSenha)
            throws SQLException {
        Long id = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        validarSenhaAtualInformada(senhaAtual);
        ServiceValidationUtils.validatePassword(novaSenha);
        ServiceValidationUtils.validatePasswordConfirmation(novaSenha, confirmacaoNovaSenha);

        Usuario usuarioExistente = buscarUsuarioExistente(id);
        if (!passwordHasher.verificar(senhaAtual, usuarioExistente.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual informada esta incorreta.");
        }

        if (passwordHasher.verificar(novaSenha, usuarioExistente.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha atual.");
        }

        usuarioDAO.atualizarSenha(id, passwordHasher.gerarHash(novaSenha));
    }

    public void desativar(Long usuarioId) throws SQLException {
        alterarStatus(usuarioId, false);
    }

    public void reativar(Long usuarioId) throws SQLException {
        alterarStatus(usuarioId, true);
    }

    private void alterarStatus(Long usuarioId, boolean ativo) throws SQLException {
        Long id = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        Usuario usuarioExistente = buscarUsuarioExistente(id);
        if (usuarioExistente.isAtivo() == ativo) {
            return;
        }

        usuarioDAO.atualizarStatus(id, ativo);
    }

    private Usuario buscarUsuarioExistente(Long usuarioId) throws SQLException {
        return usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Usuario nao encontrado."));
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

    private void validarSenhaAtualInformada(String senhaAtual) {
        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new ValidacaoException("A senha atual e obrigatoria.");
        }
    }
}
