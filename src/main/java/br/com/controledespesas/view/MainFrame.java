package br.com.controledespesas.view;

import br.com.controledespesas.view.contract.MainView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame implements MainView {

    private static final String PAINEL_INICIO = "inicio";
    private static final String PAINEL_CATEGORIAS = "categorias";
    private static final String PAINEL_CONTAS = "contas";
    private static final String PAINEL_TRANSACOES = "transacoes";
    private static final String PAINEL_COFRINHOS = "cofrinhos";

    private final JLabel nomeUsuarioLabel;
    private final JLabel emailUsuarioLabel;
    private final JLabel secaoAtualLabel;
    private final JButton sairButton;
    private final JButton inicioButton;
    private final JButton transacoesButton;
    private final JButton contasButton;
    private final JButton categoriasButton;
    private final JButton cofrinhosButton;
    private final JPanel conteudoCentralPanel;
    private final CardLayout conteudoCentralLayout;
    private final Map<String, JButton> menuButtons;
    private final Map<String, JPanel> paineisRegistrados;
    private final Map<String, String> titulosSecao;

    public MainFrame() {
        this("", "");
    }

    public MainFrame(String nomeUsuario, String emailUsuario) {
        super("Sistema de Controle de Despesas Pessoais");
        nomeUsuarioLabel = new JLabel(nomeUsuario);
        emailUsuarioLabel = new JLabel(emailUsuario);
        secaoAtualLabel = new JLabel("Inicio");
        sairButton = new JButton("Sair");
        inicioButton = criarBotaoMenu("Inicio");
        transacoesButton = criarBotaoMenu("Transacoes");
        contasButton = criarBotaoMenu("Contas");
        categoriasButton = criarBotaoMenu("Categorias");
        cofrinhosButton = criarBotaoMenu("Cofrinhos");
        conteudoCentralLayout = new CardLayout();
        conteudoCentralPanel = new JPanel(conteudoCentralLayout);
        menuButtons = new LinkedHashMap<>();
        paineisRegistrados = new LinkedHashMap<>();
        titulosSecao = new LinkedHashMap<>();
        initialize();
        exibirUsuario(nomeUsuario, emailUsuario);
    }

    @Override
    public void exibirUsuario(String nome, String email) {
        String nomeSeguro = nome != null && !nome.isBlank() ? nome : "Usuario";
        String emailSeguro = email != null ? email : "";

        nomeUsuarioLabel.setText(nomeSeguro);
        emailUsuarioLabel.setText(emailSeguro);
    }

    @Override
    public void adicionarPainel(String identificador, JPanel painel) {
        if (identificador == null || identificador.isBlank() || painel == null) {
            return;
        }

        JPanel painelAnterior = paineisRegistrados.get(identificador);
        if (painelAnterior == painel) {
            return;
        }

        if (painelAnterior != null) {
            conteudoCentralPanel.remove(painelAnterior);
        }

        paineisRegistrados.put(identificador, painel);
        conteudoCentralPanel.add(painel, identificador);
        conteudoCentralPanel.revalidate();
        conteudoCentralPanel.repaint();
    }

    @Override
    public void mostrarPainel(String identificador) {
        if (!paineisRegistrados.containsKey(identificador)) {
            return;
        }

        conteudoCentralLayout.show(conteudoCentralPanel, identificador);
        atualizarTituloSecao(identificador);
    }

    @Override
    public void definirMenuAtivo(String identificador) {
        for (Map.Entry<String, JButton> entry : menuButtons.entrySet()) {
            aplicarEstiloMenu(entry.getValue(), entry.getKey().equals(identificador));
        }
        atualizarTituloSecao(identificador);
    }

    @Override
    public void definirAcaoInicio(Runnable acao) {
        configurarAcao(inicioButton, acao);
    }

    @Override
    public void definirAcaoCategorias(Runnable acao) {
        configurarAcao(categoriasButton, acao);
    }

    @Override
    public void definirAcaoContas(Runnable acao) {
        configurarAcao(contasButton, acao);
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
        registrarTitulosSecao();
        registrarMenuButtons();
        configurarBotoesFuturos();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 620));
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
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));

        sidebar.add(inicioButton);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(transacoesButton);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(contasButton);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(categoriasButton);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(cofrinhosButton);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(UiStyles.WHITE);
        content.setBorder(UiStyles.createCardBorder());

        JPanel secaoPanel = new JPanel(new BorderLayout());
        secaoPanel.setOpaque(false);

        secaoAtualLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        secaoAtualLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel resumoLabel = new JLabel("Navegue entre os modulos sem abrir novas janelas.");
        resumoLabel.setFont(UiStyles.SUBTITLE_FONT);
        resumoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel secaoTexto = new JPanel();
        secaoTexto.setOpaque(false);
        secaoTexto.setLayout(new BoxLayout(secaoTexto, BoxLayout.Y_AXIS));
        secaoTexto.add(secaoAtualLabel);
        secaoTexto.add(Box.createVerticalStrut(4));
        secaoTexto.add(resumoLabel);

        secaoPanel.add(secaoTexto, BorderLayout.WEST);

        conteudoCentralPanel.setOpaque(false);

        content.add(secaoPanel, BorderLayout.NORTH);
        content.add(conteudoCentralPanel, BorderLayout.CENTER);
        return content;
    }

    private JButton criarBotaoMenu(String texto) {
        JButton button = new JButton(texto);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        button.setOpaque(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        aplicarEstiloMenu(button, false);
        return button;
    }

    private void registrarTitulosSecao() {
        titulosSecao.put(PAINEL_INICIO, "Inicio");
        titulosSecao.put(PAINEL_CATEGORIAS, "Categorias");
        titulosSecao.put(PAINEL_CONTAS, "Contas");
        titulosSecao.put(PAINEL_TRANSACOES, "Transacoes");
        titulosSecao.put(PAINEL_COFRINHOS, "Cofrinhos");
    }

    private void registrarMenuButtons() {
        menuButtons.put(PAINEL_INICIO, inicioButton);
        menuButtons.put(PAINEL_CONTAS, contasButton);
        menuButtons.put(PAINEL_CATEGORIAS, categoriasButton);
    }

    private void configurarBotoesFuturos() {
        transacoesButton.setEnabled(false);
        transacoesButton.setToolTipText("Disponivel nas proximas etapas.");
        cofrinhosButton.setEnabled(false);
        cofrinhosButton.setToolTipText("Disponivel nas proximas etapas.");
        aplicarEstiloPlaceholder(transacoesButton);
        aplicarEstiloPlaceholder(cofrinhosButton);
    }

    private void atualizarTituloSecao(String identificador) {
        secaoAtualLabel.setText(titulosSecao.getOrDefault(identificador, "Inicio"));
    }

    private void configurarAcao(JButton button, Runnable acao) {
        for (var listener : button.getActionListeners()) {
            button.removeActionListener(listener);
        }

        if (acao != null) {
            button.addActionListener(event -> acao.run());
        }
    }

    private void aplicarEstiloMenu(JButton button, boolean ativo) {
        if (!button.isEnabled()) {
            aplicarEstiloPlaceholder(button);
            return;
        }

        button.setFont(new Font(Font.SANS_SERIF, ativo ? Font.BOLD : Font.PLAIN, 14));
        button.setBackground(ativo ? UiStyles.PRIMARY : UiStyles.BACKGROUND);
        button.setForeground(ativo ? UiStyles.WHITE : UiStyles.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ativo ? UiStyles.PRIMARY : UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void aplicarEstiloPlaceholder(JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        button.setBackground(UiStyles.BACKGROUND);
        button.setForeground(UiStyles.TEXT_SECONDARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
    }
}
