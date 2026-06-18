package br.com.controledespesas.controller;

import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.util.SqlExceptionUtils;
import br.com.controledespesas.util.ValidationUtils;
import br.com.controledespesas.view.contract.CadastroUsuarioView;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordena o cadastro de novos usuarios e suas validacoes de formulario.
 */
public class CadastroUsuarioController {

    private static final Logger LOGGER = Logger.getLogger(CadastroUsuarioController.class.getName());
    private static final int MAX_NOME = 150;
    private static final String MENSAGEM_SUCESSO = "Conta criada com sucesso. Agora voce ja pode entrar.";
    private static final String MENSAGEM_EMAIL_DUPLICADO = "Ja existe um usuario cadastrado com este e-mail.";
    private static final String MENSAGEM_ERRO_BANCO =
            "Nao foi possivel acessar o banco de dados. Tente novamente.";
    private static final String MENSAGEM_ERRO_INESPERADO =
            "Ocorreu um erro inesperado. Tente novamente.";

    private final UsuarioDAO usuarioDAO;
    private final PasswordHasher passwordHasher;
    private final CadastroUsuarioView cadastroUsuarioView;
    private final ApplicationController applicationController;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final Consumer<Usuario> cadastroSuccessHandler;
    private final Runnable voltarHandler;

    public CadastroUsuarioController(UsuarioDAO usuarioDAO, PasswordHasher passwordHasher,
                                     CadastroUsuarioView cadastroUsuarioView,
                                     ApplicationController applicationController, AsyncTaskExecutor asyncTaskExecutor) {
        this(usuarioDAO, passwordHasher, cadastroUsuarioView, applicationController, asyncTaskExecutor, null, null);
    }

    public CadastroUsuarioController(UsuarioDAO usuarioDAO, PasswordHasher passwordHasher,
                                     CadastroUsuarioView cadastroUsuarioView,
                                     ApplicationController applicationController, AsyncTaskExecutor asyncTaskExecutor,
                                     Consumer<Usuario> cadastroSuccessHandler, Runnable voltarHandler) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO nao pode ser nulo.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher nao pode ser nulo.");
        this.cadastroUsuarioView =
                Objects.requireNonNull(cadastroUsuarioView, "cadastroUsuarioView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        this.cadastroSuccessHandler = cadastroSuccessHandler;
        this.voltarHandler = voltarHandler;

        this.cadastroUsuarioView.setCadastrarAction(this::cadastrar);
        this.cadastroUsuarioView.setVoltarAction(this::voltarParaLogin);
        carregarUsuariosSeDisponivel();
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
        if (voltarHandler != null) {
            voltarHandler.run();
            return;
        }
        applicationController.mostrarLogin();
    }

    private Usuario cadastrarUsuario(String nome, String email, char[] senhaParaProcessamento,
                                     char[] confirmacaoParaProcessamento) throws SQLException {
        String senha = new String(senhaParaProcessamento);
        String confirmacao = new String(confirmacaoParaProcessamento);

        try {
            String nomeNormalizado = ValidationUtils.normalizeRequiredText(nome, "Nome", MAX_NOME);
            String emailNormalizado = ValidationUtils.normalizeEmail(email);
            ValidationUtils.validatePassword(senha);
            ValidationUtils.validatePasswordConfirmation(senha, confirmacao);

            if (usuarioDAO.emailExiste(emailNormalizado)) {
                throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO);
            }

            Usuario usuario = new Usuario();
            usuario.setNome(nomeNormalizado);
            usuario.setEmail(emailNormalizado);
            usuario.setSenhaHash(passwordHasher.gerarHash(senha));
            usuario.setAtivo(true);

            try {
                usuarioDAO.inserir(usuario);
                return sanitizarUsuario(usuario);
            } catch (SQLException exception) {
                if (SqlExceptionUtils.isDuplicateKey(exception)) {
                    throw new RegraNegocioException(MENSAGEM_EMAIL_DUPLICADO, exception);
                }
                throw exception;
            } finally {
                usuario.setSenhaHash(null);
            }
        } finally {
            Arrays.fill(senhaParaProcessamento, '\0');
            Arrays.fill(confirmacaoParaProcessamento, '\0');
        }
    }

    private void onCadastroSuccess(Usuario usuario) {
        carregarUsuariosSeDisponivel();
        if (cadastroSuccessHandler != null) {
            cadastroSuccessHandler.accept(usuario);
            return;
        }
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

    private void carregarUsuariosSeDisponivel() {
        if (!cadastroUsuarioView.suportaListagemUsuarios()) {
            return;
        }

        asyncTaskExecutor.execute(
                usuarioDAO::listarTodos,
                cadastroUsuarioView::exibirUsuarios,
                this::onCadastroError,
                () -> {
                }
        );
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
