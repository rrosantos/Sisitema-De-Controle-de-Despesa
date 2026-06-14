package br.com.controledespesas.view;

import br.com.controledespesas.view.contract.CadastroUsuarioView;

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

public class CadastroUsuarioPanel extends JPanel implements CadastroUsuarioView {

    private final JTextField nomeField;
    private final JTextField emailField;
    private final JPasswordField senhaField;
    private final JPasswordField confirmacaoSenhaField;
    private final JButton cadastrarButton;
    private final JButton voltarButton;
    private final JLabel mensagemLabel;

    private Runnable cadastrarAction;
    private Runnable voltarAction;

    public CadastroUsuarioPanel() {
        setLayout(new BorderLayout());
        setBackground(UiStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        nomeField = new JTextField();
        emailField = new JTextField();
        senhaField = new JPasswordField();
        confirmacaoSenhaField = new JPasswordField();
        cadastrarButton = new JButton("Cadastrar");
        voltarButton = new JButton("Voltar para o login");
        mensagemLabel = UiStyles.createMessageLabel();

        UiStyles.styleTextComponent(nomeField);
        UiStyles.styleTextComponent(emailField);
        UiStyles.styleTextComponent(senhaField);
        UiStyles.styleTextComponent(confirmacaoSenhaField);
        UiStyles.stylePrimaryButton(cadastrarButton);
        UiStyles.styleSecondaryButton(voltarButton);

        add(criarCard(), BorderLayout.CENTER);
        configurarEventos();
    }

    @Override
    public String getNome() {
        return nomeField.getText();
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
    public char[] getConfirmacaoSenha() {
        return confirmacaoSenhaField.getPassword();
    }

    @Override
    public void limparCampos() {
        nomeField.setText("");
        emailField.setText("");
        senhaField.setText("");
        confirmacaoSenhaField.setText("");
        limparMensagem();
    }

    @Override
    public void limparSenhas() {
        senhaField.setText("");
        confirmacaoSenhaField.setText("");
    }

    @Override
    public void setCarregando(boolean carregando) {
        nomeField.setEnabled(!carregando);
        emailField.setEnabled(!carregando);
        senhaField.setEnabled(!carregando);
        confirmacaoSenhaField.setEnabled(!carregando);
        cadastrarButton.setEnabled(!carregando);
        voltarButton.setEnabled(!carregando);
        cadastrarButton.setText(carregando ? "Cadastrando..." : "Cadastrar");
    }

    @Override
    public void focarNome() {
        SwingUtilities.invokeLater(() -> nomeField.requestFocusInWindow());
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
    public void setCadastrarAction(Runnable action) {
        this.cadastrarAction = action;
    }

    @Override
    public void setVoltarAction(Runnable action) {
        this.voltarAction = action;
    }

    JButton getPrimaryButton() {
        return cadastrarButton;
    }

    private JPanel criarCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiStyles.WHITE);
        card.setBorder(UiStyles.createCardBorder());
        card.setPreferredSize(new Dimension(430, 470));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 14, 0);

        JLabel titulo = new JLabel("Criar conta");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Cadastre-se para comecar a organizar suas despesas.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        card.add(titulo, constraints);
        constraints.gridy++;
        card.add(subtitulo, constraints);

        constraints.insets = new Insets(12, 0, 6, 0);
        constraints.gridy++;
        card.add(criarCampo("Nome", nomeField), constraints);

        constraints.gridy++;
        card.add(criarCampo("E-mail", emailField), constraints);

        constraints.gridy++;
        card.add(criarCampo("Senha", senhaField), constraints);

        constraints.gridy++;
        card.add(criarCampo("Confirmacao de senha", confirmacaoSenhaField), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(10, 0, 0, 0);
        card.add(mensagemLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(cadastrarButton, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 0, 0);
        card.add(voltarButton, constraints);

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

    private void configurarEventos() {
        cadastrarButton.addActionListener(event -> executarCadastro());
        voltarButton.addActionListener(event -> executarVoltar());
        confirmacaoSenhaField.addActionListener(event -> executarCadastro());
    }

    private void executarCadastro() {
        if (cadastrarAction != null) {
            cadastrarAction.run();
        }
    }

    private void executarVoltar() {
        if (voltarAction != null) {
            voltarAction.run();
        }
    }
}
