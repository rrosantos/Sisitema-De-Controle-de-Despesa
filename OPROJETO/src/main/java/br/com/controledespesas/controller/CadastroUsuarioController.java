package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.UsuarioService;
import br.com.controledespesas.view.contract.CadastroUsuarioView;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CadastroUsuarioController {

    private static final Logger LOGGER = Logger.getLogger(CadastroUsuarioController.class.getName());
    private static final String MENSAGEM_SUCESSO = "Conta criada com sucesso. Agora voce ja pode entrar.";
    private static final String MENSAGEM_ERRO_BANCO =
            "Nao foi possivel acessar o banco de dados. Tente novamente.";
    private static final String MENSAGEM_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente.";

    private final UsuarioService usuarioService;
    private final CadastroUsuarioView cadastroUsuarioView;
    private final ApplicationController applicationController;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public CadastroUsuarioController(UsuarioService usuarioService, CadastroUsuarioView cadastroUsuarioView,
                                     ApplicationController applicationController, AsyncTaskExecutor asyncTaskExecutor) {
        this.usuarioService = Objects.requireNonNull(usuarioService, "usuarioService nao pode ser nulo.");
        this.cadastroUsuarioView =
                Objects.requireNonNull(cadastroUsuarioView, "cadastroUsuarioView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");

        this.cadastroUsuarioView.setCadastrarAction(this::cadastrar);
        this.cadastroUsuarioView.setVoltarAction(this::voltarParaLogin);
    }

    public void cadastrar() {
        cadastroUsuarioView.limparMensagem();
        cadastroUsuarioView.setCarregando(true);

        String nome = cadastroUsuarioView.getNome();
        String email = cadastroUsuarioView.getEmail();
        char[] senhaCapturada = cadastroUsuarioView.getSenha();
        char[] confirmacaoCapturada = cadastroUsuarioView.getConfirmacaoSenha();
        char[] senhaParaProcessamento = Arrays.copyOf(senhaCapturada, senhaCapturada.length);
        char[] confirmacaoParaProcessamento = Arrays.copyOf(confirmacaoCapturada, confirmacaoCapturada.length);
        Arrays.fill(senhaCapturada, '\0');
        Arrays.fill(confirmacaoCapturada, '\0');

        asyncTaskExecutor.execute(
                () -> cadastrarUsuario(nome, email, senhaParaProcessamento, confirmacaoParaProcessamento),
                this::onCadastroSuccess,
                this::onCadastroError,
                () -> {
                    cadastroUsuarioView.setCarregando(false);
                    cadastroUsuarioView.limparSenhas();
                }
        );
    }

    public void voltarParaLogin() {
        cadastroUsuarioView.limparMensagem();
        applicationController.mostrarLogin();
    }

    private Usuario cadastrarUsuario(String nome, String email, char[] senhaParaProcessamento,
                                     char[] confirmacaoParaProcessamento) throws SQLException {
        String senha = new String(senhaParaProcessamento);
        String confirmacao = new String(confirmacaoParaProcessamento);

        try {
            return usuarioService.cadastrar(nome, email, senha, confirmacao);
        } finally {
            Arrays.fill(senhaParaProcessamento, '\0');
            Arrays.fill(confirmacaoParaProcessamento, '\0');
        }
    }

    private void onCadastroSuccess(Usuario usuario) {
        cadastroUsuarioView.limparCampos();
        applicationController.mostrarLoginComEmail(usuario.getEmail(), MENSAGEM_SUCESSO);
    }

    private void onCadastroError(Throwable throwable) {
        if (throwable instanceof ValidacaoException exception) {
            cadastroUsuarioView.mostrarErro(exception.getMessage());
            return;
        }

        if (throwable instanceof RegraNegocioException exception) {
            cadastroUsuarioView.mostrarErro(exception.getMessage());
            return;
        }

        if (throwable instanceof SQLException exception) {
            LOGGER.log(Level.WARNING, "Falha ao cadastrar usuario na interface.", exception);
            cadastroUsuarioView.mostrarErro(MENSAGEM_ERRO_BANCO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao cadastrar usuario.", throwable);
        cadastroUsuarioView.mostrarErro(MENSAGEM_ERRO_INESPERADO);
    }
}
