package br.com.controledespesas.view;

import br.com.controledespesas.view.contract.MainView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

public class MainFrame extends JFrame implements MainView {

    private final JLabel nomeUsuarioLabel;
    private final JLabel emailUsuarioLabel;
    private final JLabel boasVindasLabel;
    private final JButton sairButton;

    public MainFrame() {
        this("", "");
    }

    public MainFrame(String nomeUsuario, String emailUsuario) {
        super("Sistema de Controle de Despesas Pessoais");
        nomeUsuarioLabel = new JLabel(nomeUsuario);
        emailUsuarioLabel = new JLabel(emailUsuario);
        boasVindasLabel = new JLabel("Bem-vinda(o)!");
        sairButton = new JButton("Sair");
        initialize();
        exibirUsuario(nomeUsuario, emailUsuario);
    }

    @Override
    public void exibirUsuario(String nome, String email) {
        String nomeSeguro = nome != null && !nome.isBlank() ? nome : "Usuario";
        String emailSeguro = email != null ? email : "";

        nomeUsuarioLabel.setText(nomeSeguro);
        emailUsuarioLabel.setText(emailSeguro);
        boasVindasLabel.setText("Bem-vinda(o), " + nomeSeguro + ".");
    }

    @Override
    public void definirAcaoSair(Runnable action) {
        for (var listener : sairButton.getActionListeners()) {
            sairButton.removeActionListener(listener);
        }
        sairButton.addActionListener(event -> action.run());
    }

    @Override
    public void abrir() {
        setVisible(true);
    }

    @Override
    public void fechar() {
        dispose();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setResizable(true);
        setContentPane(createContentPanel());
    }

    private JPanel createContentPanel() {
        JPanel rootPanel = new JPanel(new BorderLayout(24, 24));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        rootPanel.setBackground(UiStyles.BACKGROUND);

        rootPanel.add(createHeader(), BorderLayout.NORTH);
        rootPanel.add(createSidebar(), BorderLayout.WEST);
        rootPanel.add(createMainContent(), BorderLayout.CENTER);

        return rootPanel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("Sistema de Controle de Despesas");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JPanel userPanel = new JPanel();
        userPanel.setOpaque(false);
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.X_AXIS));

        JPanel userInfo = new JPanel();
        userInfo.setOpaque(false);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));

        nomeUsuarioLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        nomeUsuarioLabel.setForeground(UiStyles.TEXT_PRIMARY);

        emailUsuarioLabel.setFont(UiStyles.SMALL_FONT);
        emailUsuarioLabel.setForeground(UiStyles.TEXT_SECONDARY);

        userInfo.add(nomeUsuarioLabel);
        userInfo.add(Box.createVerticalStrut(2));
        userInfo.add(emailUsuarioLabel);

        UiStyles.styleSecondaryButton(sairButton);
        sairButton.setText("Sair");

        userPanel.add(userInfo);
        userPanel.add(Box.createHorizontalStrut(16));
        userPanel.add(sairButton);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(userPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(UiStyles.WHITE);
        sidebar.setBorder(UiStyles.createCardBorder());
        sidebar.setLayout(new GridLayout(5, 1, 0, 10));
        sidebar.setPreferredSize(new Dimension(200, 0));

        sidebar.add(createMenuButton("Inicio", true));
        sidebar.add(createMenuButton("Transacoes", false));
        sidebar.add(createMenuButton("Contas", false));
        sidebar.add(createMenuButton("Categorias", false));
        sidebar.add(createMenuButton("Cofrinhos", false));
        return sidebar;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UiStyles.WHITE);
        content.setBorder(UiStyles.createCardBorder());

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        boasVindasLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        boasVindasLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel resumoLabel = new JLabel("Sua area financeira sera implementada nas proximas etapas.");
        resumoLabel.setFont(UiStyles.SUBTITLE_FONT);
        resumoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JTextArea detailsArea = new JTextArea(
                "Nesta etapa, a aplicacao ja possui autenticacao, cadastro de usuario, " +
                "sessao compartilhada e navegacao inicial entre as telas.\n\n" +
                "Os modulos de categorias, contas, transacoes e cofrinhos aparecerao aqui " +
                "conforme avancarmos na interface principal."
        );
        detailsArea.setEditable(false);
        detailsArea.setFocusable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setOpaque(false);
        detailsArea.setForeground(UiStyles.TEXT_PRIMARY);
        detailsArea.setFont(UiStyles.TEXT_FONT);

        textPanel.add(boasVindasLabel);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(resumoLabel);
        textPanel.add(Box.createVerticalStrut(22));
        textPanel.add(detailsArea);

        content.add(textPanel, BorderLayout.CENTER);
        return content;
    }

    private JButton createMenuButton(String texto, boolean ativo) {
        JButton button = new JButton(texto);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, ativo ? Font.BOLD : Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setOpaque(true);

        if (ativo) {
            button.setBackground(UiStyles.PRIMARY);
            button.setForeground(UiStyles.WHITE);
            button.setEnabled(false);
        } else {
            button.setBackground(UiStyles.BACKGROUND);
            button.setForeground(UiStyles.TEXT_SECONDARY);
            button.setToolTipText("Disponivel na proxima etapa");
            button.setEnabled(false);
        }

        return button;
    }
}
