package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

public class InicioPanel extends JPanel {

    private final JLabel boasVindasLabel;
    private final JButton transacoesButton;
    private final JButton categoriasButton;
    private final JButton contasButton;

    public InicioPanel() {
        setLayout(new BorderLayout(24, 24));
        setOpaque(false);

        boasVindasLabel = new JLabel("Bem-vinda(o), Usuario.");
        transacoesButton = new JButton("Abrir transacoes");
        categoriasButton = new JButton("Abrir categorias");
        contasButton = new JButton("Abrir contas");

        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarCards(), BorderLayout.CENTER);
    }

    public void exibirUsuario(String nome) {
        String nomeSeguro = nome != null && !nome.isBlank() ? nome : "Usuario";
        boasVindasLabel.setText("Bem-vinda(o), " + nomeSeguro + ".");
    }

    public void definirAcaoCategorias(Runnable acao) {
        for (var listener : categoriasButton.getActionListeners()) {
            categoriasButton.removeActionListener(listener);
        }

        if (acao != null) {
            categoriasButton.addActionListener(event -> acao.run());
        }
    }

    public void definirAcaoTransacoes(Runnable acao) {
        for (var listener : transacoesButton.getActionListeners()) {
            transacoesButton.removeActionListener(listener);
        }

        if (acao != null) {
            transacoesButton.addActionListener(event -> acao.run());
        }
    }

    public void definirAcaoContas(Runnable acao) {
        for (var listener : contasButton.getActionListeners()) {
            contasButton.removeActionListener(listener);
        }

        if (acao != null) {
            contasButton.addActionListener(event -> acao.run());
        }
    }

    private JPanel criarCabecalho() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        boasVindasLabel.setFont(UiStyles.TITLE_FONT);
        boasVindasLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Organize categorias, contas e transacoes no mesmo fluxo principal.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        JTextArea texto = new JTextArea(
                "Use os atalhos abaixo para navegar entre os modulos ja disponiveis e acompanhar seu controle financeiro. " +
                        "O modulo de cofrinhos continua planejado para as proximas etapas."
        );
        texto.setEditable(false);
        texto.setFocusable(false);
        texto.setOpaque(false);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        texto.setFont(UiStyles.TEXT_FONT);
        texto.setForeground(UiStyles.TEXT_PRIMARY);

        panel.add(boasVindasLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(18));
        panel.add(texto);
        return panel;
    }

    private JPanel criarCards() {
        JPanel cards = new JPanel(new GridLayout(2, 2, 18, 18));
        cards.setOpaque(false);

        cards.add(criarCard(
                "Transacoes",
                "Registre receitas e despesas com filtros e resumos.",
                "Abrir transacoes",
                transacoesButton,
                true
        ));
        cards.add(criarCard(
                "Contas",
                "Cadastre carteira, conta-corrente, poupanca e acompanhe o saldo atual.",
                "Abrir contas",
                contasButton,
                true
        ));
        cards.add(criarCard(
                "Categorias",
                "Organize suas receitas e despesas por categorias ativas e inativas.",
                "Abrir categorias",
                categoriasButton,
                true
        ));
        cards.add(criarCard(
                "Cofrinhos",
                "Metas e progresso visual chegarao nas proximas etapas.",
                "Disponivel na proxima etapa",
                null,
                false
        ));

        return cards;
    }

    private JPanel criarCard(String titulo, String descricao, String textoAcao, JButton botaoAcao, boolean habilitado) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(UiStyles.WHITE);
        card.setBorder(UiStyles.createCardBorder());
        card.setPreferredSize(new Dimension(260, 180));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 18));
        tituloLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JTextArea descricaoArea = new JTextArea(descricao);
        descricaoArea.setEditable(false);
        descricaoArea.setFocusable(false);
        descricaoArea.setOpaque(false);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);
        descricaoArea.setFont(UiStyles.TEXT_FONT);
        descricaoArea.setForeground(UiStyles.TEXT_SECONDARY);

        text.add(tituloLabel);
        text.add(Box.createVerticalStrut(10));
        text.add(descricaoArea);

        card.add(text, BorderLayout.CENTER);

        JButton acaoButton = botaoAcao != null ? botaoAcao : new JButton(textoAcao);
        if (habilitado) {
            UiStyles.styleSecondaryButton(acaoButton);
        } else {
            UiStyles.styleSecondaryButton(acaoButton);
            acaoButton.setEnabled(false);
            acaoButton.setToolTipText("Disponivel na proxima etapa");
        }

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        footer.add(acaoButton, BorderLayout.WEST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }
}
