package br.com.controledespesas.view;

import br.com.controledespesas.view.contract.LoginView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class LoginPanel extends JPanel implements LoginView {

    private final JTextField emailField;
    private final JPasswordField senhaField;
    private final JButton entrarButton;
    private final JButton criarContaButton;
    private final JLabel mensagemLabel;

    private Runnable entrarAction;
    private Runnable criarContaAction;

    public LoginPanel() {
        setLayout(new BorderLayout());
        setBackground(UiStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        emailField = new JTextField();
        senhaField = new JPasswordField();
        entrarButton = new JButton("Entrar");
        criarContaButton = new JButton("Criar conta");
        mensagemLabel = UiStyles.createMessageLabel();

        UiStyles.styleTextComponent(emailField);
        UiStyles.styleTextComponent(senhaField);
        UiStyles.stylePrimaryButton(entrarButton);
        UiStyles.styleLinkButton(criarContaButton);

        add(criarCard(), BorderLayout.CENTER);
        configurarEventos();
    }

    @Override
    public String getEmail() {
        return emailField.getText();
    }

    @Override
    public char[] getSenha() {
        return senhaField.getPassword();
    }

    @Override
    public void limparSenha() {
        senhaField.setText("");
    }

    @Override
    public void limparCampos() {
        emailField.setText("");
        senhaField.setText("");
        limparMensagem();
    }

    @Override
    public void preencherEmail(String email) {
        emailField.setText(email != null ? email : "");
    }

    @Override
    public void setCarregando(boolean carregando) {
        entrarButton.setEnabled(!carregando);
        criarContaButton.setEnabled(!carregando);
        emailField.setEnabled(!carregando);
        senhaField.setEnabled(!carregando);
        entrarButton.setText(carregando ? "Entrando..." : "Entrar");
    }

    @Override
    public void focarEmail() {
        SwingUtilities.invokeLater(() -> emailField.requestFocusInWindow());
    }

    @Override
    public void mostrarErro(String mensagem) {
        mensagemLabel.setForeground(UiStyles.ERROR);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void mostrarSucesso(String mensagem) {
        mensagemLabel.setForeground(UiStyles.SUCCESS);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void limparMensagem() {
        mensagemLabel.setText(" ");
    }

    @Override
    public void setEntrarAction(Runnable action) {
        this.entrarAction = action;
    }

    @Override
    public void setCriarContaAction(Runnable action) {
        this.criarContaAction = action;
    }

    JButton getPrimaryButton() {
        return entrarButton;
    }

    private JPanel criarCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiStyles.WHITE);
        card.setBorder(UiStyles.createCardBorder());
        card.setPreferredSize(new Dimension(430, 380));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 14, 0);

        JLabel titulo = new JLabel("Sistema de Controle de Despesas Pessoais");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Entre para acessar sua area financeira.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        card.add(titulo, constraints);
        constraints.gridy++;
        card.add(subtitulo, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 6, 0);
        card.add(criarCampo("E-mail", emailField), constraints);

        constraints.gridy++;
        card.add(criarCampo("Senha", senhaField), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(10, 0, 0, 0);
        card.add(mensagemLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(entrarButton, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 0, 0);
        card.add(criarRodape(), constraints);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel criarCampo(String label, JTextField field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel jLabel = new JLabel(label);
        jLabel.setFont(UiStyles.LABEL_FONT);
        jLabel.setForeground(UiStyles.TEXT_PRIMARY);

        panel.add(jLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private JPanel criarRodape() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel texto = new JLabel("Ainda nao tem conta?");
        texto.setFont(UiStyles.SMALL_FONT);
        texto.setForeground(UiStyles.TEXT_SECONDARY);

        panel.add(texto);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(criarContaButton);
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private void configurarEventos() {
        entrarButton.addActionListener(event -> executarEntrar());
        criarContaButton.addActionListener(event -> executarCriarConta());
        emailField.addActionListener(event -> executarEntrar());
        senhaField.addActionListener(event -> executarEntrar());
    }

    private void executarEntrar() {
        if (entrarAction != null) {
            entrarAction.run();
        }
    }

    private void executarCriarConta() {
        if (criarContaAction != null) {
            criarContaAction.run();
        }
    }
}
