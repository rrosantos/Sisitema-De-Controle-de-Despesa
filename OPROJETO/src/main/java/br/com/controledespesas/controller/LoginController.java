package br.com.controledespesas.controller;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.LoginView;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String MENSAGEM_ERRO_BANCO =
            "Nao foi possivel acessar o banco de dados. Tente novamente.";
    private static final String MENSAGEM_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente.";
    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha invalidos.";

    private final UsuarioDAO usuarioDAO;
    private final PasswordHasher passwordHasher;
    private final SessaoUsuario sessaoUsuario;
    private final LoginView loginView;
    private final ApplicationController applicationController;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public LoginController(UsuarioDAO usuarioDAO, PasswordHasher passwordHasher, SessaoUsuario sessaoUsuario,
                           LoginView loginView, ApplicationController applicationController,
                           AsyncTaskExecutor asyncTaskExecutor) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO nao pode ser nulo.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.loginView = Objects.requireNonNull(loginView, "loginView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");

        this.loginView.setEntrarAction(this::entrar);
        this.loginView.setCriarContaAction(this::abrirCadastro);
    }

    public void entrar() {
        loginView.limparMensagem();
        loginView.setCarregando(true);

        String email = loginView.getEmail();
        char[] senhaCapturada = loginView.getSenha();
        char[] senhaParaProcessamento = Arrays.copyOf(senhaCapturada, senhaCapturada.length);
        Arrays.fill(senhaCapturada, '\0');

        asyncTaskExecutor.execute(
                () -> autenticar(email, senhaParaProcessamento),
                this::onLoginSuccess,
                this::onLoginError,
                () -> {
                    loginView.setCarregando(false);
                    loginView.limparSenha();
                }
        );
    }

    public void abrirCadastro() {
        loginView.limparMensagem();
        applicationController.mostrarCadastro();
    }

    private Usuario autenticar(String email, char[] senhaParaProcessamento) throws SQLException {
        String senha = new String(senhaParaProcessamento);
        try {
            String emailNormalizado = normalizeEmail(email);
            validarSenhaInformada(senha);

            Usuario usuario = usuarioDAO.buscarPorEmail(emailNormalizado)
                    .orElseThrow(() -> new AutenticacaoException(MENSAGEM_CREDENCIAIS_INVALIDAS));

            if (!usuario.isAtivo() || !passwordHasher.verificar(senha, usuario.getSenhaHash())) {
                throw new AutenticacaoException(MENSAGEM_CREDENCIAIS_INVALIDAS);
            }

            sessaoUsuario.iniciar(usuario);
            return sessaoUsuario.exigirUsuarioAutenticado();
        } finally {
            Arrays.fill(senhaParaProcessamento, '\0');
        }
    }

    private void onLoginSuccess(Usuario usuario) {
        applicationController.mostrarTelaPrincipal();
    }

    private void onLoginError(Throwable throwable) {
        if (throwable instanceof ValidacaoException exception) {
            loginView.mostrarErro(exception.getMessage());
            return;
        }

        if (throwable instanceof AutenticacaoException exception) {
            loginView.mostrarErro(exception.getMessage());
            return;
        }

        if (throwable instanceof RegraNegocioException exception) {
            loginView.mostrarErro(exception.getMessage());
            return;
        }

        if (throwable instanceof SQLException exception) {
            LOGGER.log(Level.WARNING, "Falha ao autenticar usuario na interface.", exception);
            loginView.mostrarErro(MENSAGEM_ERRO_BANCO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao autenticar usuario.", throwable);
        loginView.mostrarErro(MENSAGEM_ERRO_INESPERADO);
    }

    private String normalizeRequiredText(String valor, String nomeCampo, int tamanhoMaximo) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }

        String normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }

        if (normalizado.length() > tamanhoMaximo) {
            throw new ValidacaoException(nomeCampo + " deve ter no maximo " + tamanhoMaximo + " caracteres.");
        }

        return normalizado;
    }

    private String normalizeEmail(String email) {
        String normalizado = normalizeRequiredText(email, "E-mail", 255).toLowerCase(Locale.ROOT);
        if (!emailValido(normalizado)) {
            throw new ValidacaoException("E-mail invalido.");
        }
        return normalizado;
    }

    private boolean emailValido(String email) {
        int indiceArroba = email.indexOf('@');
        int ultimoPonto = email.lastIndexOf('.');
        return indiceArroba > 0
                && ultimoPonto > indiceArroba + 1
                && ultimoPonto < email.length() - 1
                && email.indexOf(' ') < 0;
    }

    private void validarSenhaInformada(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new ValidacaoException("A senha e obrigatoria.");
        }
    }
}
