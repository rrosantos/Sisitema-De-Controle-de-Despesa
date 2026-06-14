package br.com.controledespesas.controller;

import br.com.controledespesas.app.ApplicationContext;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.view.AuthFrame;
import br.com.controledespesas.view.CadastroUsuarioPanel;
import br.com.controledespesas.view.LoginPanel;
import br.com.controledespesas.view.MainFrame;

import java.util.Objects;

public class ApplicationController {

    private final ApplicationContext applicationContext;

    private AuthFrame authFrame;
    private MainFrame mainFrame;

    public ApplicationController(ApplicationContext applicationContext) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext nao pode ser nulo.");
    }

    public void iniciar() {
        mostrarLogin();
    }

    public void mostrarLogin() {
        garantirTelaAutenticacao();
        authFrame.mostrarLogin();
        exibirTelaAutenticacao();
    }

    public void mostrarCadastro() {
        garantirTelaAutenticacao();
        authFrame.mostrarCadastro();
        exibirTelaAutenticacao();
    }

    public void mostrarLoginComEmail(String email) {
        garantirTelaAutenticacao();
        authFrame.mostrarLoginComEmail(email);
        exibirTelaAutenticacao();
    }

    public void mostrarLoginComEmail(String email, String mensagemSucesso) {
        garantirTelaAutenticacao();
        authFrame.mostrarLoginComEmail(email, mensagemSucesso);
        exibirTelaAutenticacao();
    }

    public void mostrarTelaPrincipal() {
        descartarTelaAutenticacao();
        descartarTelaPrincipal();

        Usuario usuario = applicationContext.getSessaoUsuario().getUsuarioAtual().orElse(null);
        String nome = usuario != null ? usuario.getNome() : "";
        String email = usuario != null ? usuario.getEmail() : "";

        mainFrame = new MainFrame(nome, email);
        MainController mainController = new MainController(
                applicationContext.getAutenticacaoService(),
                applicationContext.getSessaoUsuario(),
                mainFrame,
                this
        );
        mainController.iniciar();
    }

    public void realizarLogout() {
        descartarTelaPrincipal();
        mostrarLogin();
    }

    private void garantirTelaAutenticacao() {
        if (authFrame != null && authFrame.isDisplayable()) {
            return;
        }

        LoginPanel loginPanel = new LoginPanel();
        CadastroUsuarioPanel cadastroUsuarioPanel = new CadastroUsuarioPanel();

        authFrame = new AuthFrame(loginPanel, cadastroUsuarioPanel);

        new LoginController(
                applicationContext.getAutenticacaoService(),
                loginPanel,
                this,
                applicationContext.getAsyncTaskExecutor()
        );
        new CadastroUsuarioController(
                applicationContext.getUsuarioService(),
                cadastroUsuarioPanel,
                this,
                applicationContext.getAsyncTaskExecutor()
        );
    }

    private void exibirTelaAutenticacao() {
        if (authFrame == null) {
            return;
        }

        authFrame.setLocationRelativeTo(null);
        authFrame.setVisible(true);
        authFrame.toFront();
    }

    private void descartarTelaAutenticacao() {
        if (authFrame != null) {
            authFrame.dispose();
            authFrame = null;
        }
    }

    private void descartarTelaPrincipal() {
        if (mainFrame != null) {
            mainFrame.dispose();
            mainFrame = null;
        }
    }
}
