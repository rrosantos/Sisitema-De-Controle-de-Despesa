package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.view.contract.LoginView;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String MENSAGEM_ERRO_BANCO =
            "Nao foi possivel acessar o banco de dados. Tente novamente.";
    private static final String MENSAGEM_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente.";

    private final AutenticacaoService autenticacaoService;
    private final LoginView loginView;
    private final ApplicationController applicationController;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public LoginController(AutenticacaoService autenticacaoService, LoginView loginView,
                           ApplicationController applicationController, AsyncTaskExecutor asyncTaskExecutor) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
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
            return autenticacaoService.autenticar(email, senha);
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
}
