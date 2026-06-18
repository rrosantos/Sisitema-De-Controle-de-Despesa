package br.com.controledespesas.view;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Define responsabilidades de AuthFrame dentro do sistema.
 */
public class AuthFrame extends JFrame {

    private static final String CARD_LOGIN = "login";
    private static final String CARD_CADASTRO = "cadastro";

    private final CardLayout cardLayout;
    private final JPanel cardsPanel;
    private final LoginPanel loginPanel;
    private final CadastroUsuarioPanel cadastroUsuarioPanel;

    public AuthFrame(LoginPanel loginPanel, CadastroUsuarioPanel cadastroUsuarioPanel) {
        super("Sistema de Controle de Despesas Pessoais");
        this.loginPanel = loginPanel;
        this.cadastroUsuarioPanel = cadastroUsuarioPanel;
        this.cardLayout = new CardLayout();
        this.cardsPanel = new JPanel(cardLayout);
        initialize();
    }

    public void mostrarLogin() {
        loginPanel.limparSenha();
        cardLayout.show(cardsPanel, CARD_LOGIN);
        getRootPane().setDefaultButton(loginPanel.getPrimaryButton());
        SwingUtilities.invokeLater(loginPanel::focarEmail);
    }

    public void mostrarCadastro() {
        cadastroUsuarioPanel.limparSenhas();
        cardLayout.show(cardsPanel, CARD_CADASTRO);
        getRootPane().setDefaultButton(cadastroUsuarioPanel.getPrimaryButton());
        SwingUtilities.invokeLater(cadastroUsuarioPanel::focarNome);
    }

    public void mostrarLoginComEmail(String email) {
        loginPanel.preencherEmail(email);
        mostrarLogin();
    }

    public void mostrarLoginComEmail(String email, String mensagemSucesso) {
        loginPanel.preencherEmail(email);
        loginPanel.mostrarSucesso(mensagemSucesso);
        cardLayout.show(cardsPanel, CARD_LOGIN);
        getRootPane().setDefaultButton(loginPanel.getPrimaryButton());
        SwingUtilities.invokeLater(loginPanel::focarEmail);
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(530, 610));
        setMinimumSize(new Dimension(500, 560));
        setResizable(true);

        cardsPanel.add(loginPanel, CARD_LOGIN);
        cardsPanel.add(cadastroUsuarioPanel, CARD_CADASTRO);
        setContentPane(cardsPanel);

        pack();
        setLocationRelativeTo(null);
    }
}
