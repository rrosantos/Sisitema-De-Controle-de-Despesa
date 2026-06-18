package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.view.component.EmptyStatePanel;
import br.com.controledespesas.view.component.LoadingPanel;
import br.com.controledespesas.view.contract.CategoriaView;
import br.com.controledespesas.view.contract.DadosCategoriaForm;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Monta e atualiza a tela Swing do modulo de Categoria.
 */
public class CategoriaPanel extends JPanel implements CategoriaView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";
    private static final int COLUMN_ACTIONS = 4;
    private static final Color FUNDO_CATEGORIAS = new Color(0xF5F7FB);
    private static final Color FUNDO_DESTAQUE = new Color(0xEEF4FF);
    private static final Color AZUL_DESTAQUE = new Color(0x2F6FED);
    private static final Color VERDE_DESTAQUE = new Color(0x15803D);
    private static final Color VERMELHO_DESTAQUE = new Color(0xB91C1C);

    private final JButton novaCategoriaButton;
    private final JComboBox<String> filtroTipoComboBox;
    private final JComboBox<String> filtroStatusComboBox;
    private final JTextField pesquisaField;
    private final JLabel mensagemLabel;
    private final DashboardSummaryCard totalCategoriasCard;
    private final DashboardSummaryCard receitasCard;
    private final DashboardSummaryCard despesasCard;
    private final CategoriaTableModel tableModel;
    private final JTable tabela;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final EmptyStatePanel emptyStatePanel;

    private final List<Categoria> categoriasOriginais = new ArrayList<>();
    private CategoriaFormDialog formularioAtual;

    private Runnable novaCategoriaAction;
    private Consumer<Categoria> editarAction;
    private Consumer<Categoria> alterarStatusAction;
    private Consumer<Categoria> excluirAction;
    private boolean carregando;

    public CategoriaPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(FUNDO_CATEGORIAS);

        novaCategoriaButton = new JButton("Nova categoria");
        filtroTipoComboBox = new JComboBox<>(new String[]{"Todas", "Receita", "Despesa"});
        filtroStatusComboBox = new JComboBox<>(new String[]{"Todas", "Ativas", "Inativas"});
        pesquisaField = new JTextField();
        mensagemLabel = UiStyles.createMessageLabel();
        totalCategoriasCard = new DashboardSummaryCard("Categorias exibidas");
        receitasCard = new DashboardSummaryCard("Categorias de receita");
        despesasCard = new DashboardSummaryCard("Categorias de despesa");
        tableModel = new CategoriaTableModel();
        tabela = new JTable(tableModel);
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        emptyStatePanel = new EmptyStatePanel(
                "Nenhuma categoria cadastrada.",
                "Crie categorias para organizar suas receitas e despesas.",
                "Nova categoria"
        );

        UiStyles.stylePrimaryButton(novaCategoriaButton);
        UiStyles.styleTextComponent(pesquisaField);
        UiStyles.styleComboBox(filtroTipoComboBox);
        UiStyles.styleComboBox(filtroStatusComboBox);
        configurarCardsResumo();
        configurarCamposFiltro();
        emptyStatePanel.setAcao(this::executarNovaCategoria);

        add(criarScrollPane(), BorderLayout.CENTER);
        configurarTabela();
        configurarFiltros();
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirCategorias(List<Categoria> categorias) {
        categoriasOriginais.clear();
        if (categorias != null) {
            categoriasOriginais.addAll(categorias);
        }
        aplicarFiltros();
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        this.carregando = carregando;
        novaCategoriaButton.setEnabled(!carregando);
        filtroTipoComboBox.setEnabled(!carregando);
        filtroStatusComboBox.setEnabled(!carregando);
        pesquisaField.setEnabled(!carregando);
        tabela.setEnabled(!carregando);
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirMensagemSucesso(String mensagem) {
        mensagemLabel.setForeground(UiStyles.SUCCESS);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void exibirMensagemErro(String mensagem) {
        mensagemLabel.setForeground(UiStyles.ERROR);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void exibirEstadoVazio() {
        categoriasOriginais.clear();
        tableModel.atualizarCategorias(List.of());
        atualizarEstadoConteudo();
    }

    @Override
    public void abrirFormularioCadastro(Consumer<DadosCategoriaForm> aoSalvar) {
        abrirFormulario("Nova categoria", null, aoSalvar);
    }

    @Override
    public void abrirFormularioEdicao(Categoria categoria, Consumer<DadosCategoriaForm> aoSalvar) {
        abrirFormulario(
                "Editar categoria",
                new DadosCategoriaForm(categoria.getNome(), categoria.getTipo(), categoria.getDescricao()),
                aoSalvar
        );
    }

    @Override
    public void fecharFormulario() {
        if (formularioAtual != null) {
            formularioAtual.fechar();
            formularioAtual = null;
        }
    }

    @Override
    public void exibirErroFormulario(String mensagem) {
        if (formularioAtual != null) {
            formularioAtual.exibirErro(mensagem);
        }
    }

    @Override
    public boolean confirmarExclusao(Categoria categoria) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja excluir a categoria \"" + categoria.getNome() + "\"?\nEssa acao nao podera ser desfeita.",
                "Confirmar exclusao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmarAlteracaoStatus(Categoria categoria, boolean novoStatus) {
        String acao = novoStatus ? "ativar" : "inativar";
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja " + acao + " a categoria \"" + categoria.getNome() + "\"?",
                "Confirmar alteracao de status",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public void definirAcaoNovaCategoria(Runnable acao) {
        this.novaCategoriaAction = acao;
    }

    @Override
    public void definirAcaoEditar(Consumer<Categoria> acao) {
        this.editarAction = acao;
    }

    @Override
    public void definirAcaoAlterarStatus(Consumer<Categoria> acao) {
        this.alterarStatusAction = acao;
    }

    @Override
    public void definirAcaoExcluir(Consumer<Categoria> acao) {
        this.excluirAction = acao;
    }

    @Override
    public void limparMensagem() {
        mensagemLabel.setText(" ");
    }

    private JPanel criarCabecalho() {
        JPanel wrapper = new JPanel(new BorderLayout(24, 0));
        wrapper.setBackground(UiStyles.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(24, 26, 24, 26)
        ));

        JPanel faixaDestaque = new JPanel();
        faixaDestaque.setBackground(AZUL_DESTAQUE);
        faixaDestaque.setPreferredSize(new Dimension(6, 0));
        wrapper.add(faixaDestaque, BorderLayout.WEST);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Categorias");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Organize receitas e despesas com categorias claras e faceis de encontrar.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel observacaoPanel = new JPanel(new BorderLayout(10, 0));
        observacaoPanel.setBackground(FUNDO_DESTAQUE);
        observacaoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD9E6FF)),
                BorderFactory.createEmptyBorder(11, 13, 11, 13)
        ));

        JLabel indicador = new JLabel("i", SwingConstants.CENTER);
        indicador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        indicador.setForeground(AZUL_DESTAQUE);
        indicador.setPreferredSize(new Dimension(22, 22));

        JLabel observacao = new JLabel("Categorias ativas ficam disponiveis nos formularios de transacoes.");
        observacao.setFont(UiStyles.SMALL_FONT);
        observacao.setForeground(UiStyles.TEXT_PRIMARY);

        observacaoPanel.add(indicador, BorderLayout.WEST);
        observacaoPanel.add(observacao, BorderLayout.CENTER);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(7));
        titlePanel.add(subtitulo);
        titlePanel.add(Box.createVerticalStrut(16));
        titlePanel.add(observacaoPanel);

        wrapper.add(titlePanel, BorderLayout.CENTER);
        wrapper.add(novaCategoriaButton, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel criarResumo() {
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 14);
        cards.add(totalCategoriasCard, gbc);

        gbc.gridx = 1;
        cards.add(receitasCard, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        cards.add(despesasCard, gbc);

        return cards;
    }

    private JPanel criarFiltros() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(UiStyles.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER));

        JPanel tituloPanel = new JPanel();
        tituloPanel.setOpaque(false);
        tituloPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        tituloPanel.setLayout(new BoxLayout(tituloPanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Filtros de categorias");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Filtre por tipo, status ou busque pelo nome da categoria.");
        descricao.setFont(UiStyles.SMALL_FONT);
        descricao.setForeground(UiStyles.TEXT_SECONDARY);

        tituloPanel.add(titulo);
        tituloPanel.add(Box.createVerticalStrut(4));
        tituloPanel.add(descricao);

        cabecalho.add(tituloPanel, BorderLayout.CENTER);
        panel.add(cabecalho, BorderLayout.NORTH);
        panel.add(criarFormularioFiltros(), BorderLayout.CENTER);
        panel.add(mensagemLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel criarFormularioFiltros() {
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 12);

        gbc.gridx = 0;
        filtros.add(criarLabeled("Tipo", filtroTipoComboBox), gbc);

        gbc.gridx = 1;
        filtros.add(criarLabeled("Status", filtroStatusComboBox), gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        filtros.add(criarLabeled("Pesquisar por nome", pesquisaField), gbc);

        return filtros;
    }

    private JPanel criarLabeled(String label, Component component) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel jLabel = new JLabel(label);
        jLabel.setFont(UiStyles.LABEL_FONT);
        jLabel.setForeground(UiStyles.TEXT_PRIMARY);

        panel.add(jLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(component);
        return panel;
    }

    private JPanel criarConteudo() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setBackground(UiStyles.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 24, 24)
        ));

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Lista de categorias");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Acoes de editar, ativar, inativar e excluir ficam no menu Acoes.");
        descricao.setFont(UiStyles.SMALL_FONT);
        descricao.setForeground(UiStyles.TEXT_SECONDARY);

        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(4));
        cabecalho.add(descricao);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(tabela);
        UiStyles.styleTableScrollPane(scrollPane);
        tabelaPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(tabelaPanel, CARD_LISTA);
        contentPanel.add(emptyStatePanel, CARD_VAZIO);
        contentPanel.add(new LoadingPanel(), CARD_LOADING);

        wrapper.add(cabecalho, BorderLayout.NORTH);
        wrapper.add(contentPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private void configurarTabela() {
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowSelectionAllowed(true);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(38);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setFont(UiStyles.TEXT_FONT);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        UiStyles.styleTable(tabela);

        JTableHeader header = tabela.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(UiStyles.WHITE);
        header.setForeground(UiStyles.TEXT_PRIMARY);
        header.setFont(UiStyles.LABEL_FONT);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER));

        DefaultTableCellRenderer buttonRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText("Acoes");
                setHorizontalAlignment(CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                setBackground(isSelected ? new java.awt.Color(0xE8F0FF) : UiStyles.WHITE);
                setForeground(UiStyles.TEXT_PRIMARY);
                setText("Acoes");
                setHorizontalAlignment(CENTER);
                return this;
            }
        };
        tabela.getColumnModel().getColumn(COLUMN_ACTIONS).setCellRenderer(buttonRenderer);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (carregando || !SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }

                int row = tabela.rowAtPoint(event.getPoint());
                int column = tabela.columnAtPoint(event.getPoint());
                if (row < 0 || column != COLUMN_ACTIONS) {
                    return;
                }

                tabela.setRowSelectionInterval(row, row);
                Categoria categoria = tableModel.getCategoriaAt(row);
                mostrarMenuAcoes(categoria, event.getComponent(), event.getX(), event.getY());
            }
        });

        novaCategoriaButton.addActionListener(event -> executarNovaCategoria());
    }

    private void configurarFiltros() {
        filtroTipoComboBox.addActionListener(event -> aplicarFiltros());
        filtroStatusComboBox.addActionListener(event -> aplicarFiltros());
        pesquisaField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltros();
            }
        });
    }

    private void aplicarFiltros() {
        List<Categoria> filtradas = categoriasOriginais.stream()
                .filter(this::filtrarPorTipo)
                .filter(this::filtrarPorStatus)
                .filter(this::filtrarPorPesquisa)
                .toList();

        tableModel.atualizarCategorias(filtradas);
        atualizarResumo(filtradas);
        atualizarEstadoConteudo();
    }

    private boolean filtrarPorTipo(Categoria categoria) {
        String filtro = (String) filtroTipoComboBox.getSelectedItem();
        if ("Receita".equals(filtro)) {
            return categoria.getTipo() == TipoCategoria.RECEITA;
        }
        if ("Despesa".equals(filtro)) {
            return categoria.getTipo() == TipoCategoria.DESPESA;
        }
        return true;
    }

    private boolean filtrarPorStatus(Categoria categoria) {
        String filtro = (String) filtroStatusComboBox.getSelectedItem();
        if ("Ativas".equals(filtro)) {
            return categoria.isAtivo();
        }
        if ("Inativas".equals(filtro)) {
            return !categoria.isAtivo();
        }
        return true;
    }

    private boolean filtrarPorPesquisa(Categoria categoria) {
        String termo = pesquisaField.getText();
        if (termo == null || termo.isBlank()) {
            return true;
        }

        return categoria.getNome().toLowerCase(Locale.ROOT).contains(termo.trim().toLowerCase(Locale.ROOT));
    }

    private void mostrarMenuAcoes(Categoria categoria, Component component, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();

        JButton editarButton = criarMenuButton("Editar", () -> {
            if (editarAction != null) {
                editarAction.accept(categoria);
            }
        });

        String textoStatus = categoria.isAtivo() ? "Inativar" : "Ativar";
        JButton statusButton = criarMenuButton(textoStatus, () -> {
            if (alterarStatusAction != null) {
                alterarStatusAction.accept(categoria);
            }
        });

        JButton excluirButton = criarMenuButton("Excluir", () -> {
            if (excluirAction != null) {
                excluirAction.accept(categoria);
            }
        });

        popupMenu.add(editarButton);
        popupMenu.add(statusButton);
        popupMenu.add(excluirButton);
        popupMenu.show(component, x, y);
    }

    private JButton criarMenuButton(String texto, Runnable acao) {
        JButton button = new JButton(texto);
        button.setHorizontalAlignment(JButton.LEFT);
        UiStyles.styleSecondaryButton(button);
        button.addActionListener(event -> acao.run());
        return button;
    }

    private void atualizarEstadoConteudo() {
        if (carregando) {
            contentLayout.show(contentPanel, CARD_LOADING);
            return;
        }

        if (tableModel.getRowCount() == 0) {
            if (categoriasOriginais.isEmpty()) {
                emptyStatePanel.setConteudo(
                        "Nenhuma categoria cadastrada.",
                        "Crie categorias para organizar suas receitas e despesas."
                );
            } else {
                emptyStatePanel.setConteudo(
                        "Nenhuma categoria encontrada.",
                        "Ajuste os filtros ou cadastre uma nova categoria."
                );
            }
            contentLayout.show(contentPanel, CARD_VAZIO);
            return;
        }

        contentLayout.show(contentPanel, CARD_LISTA);
    }

    private JScrollPane criarScrollPane() {
        DashboardContentPanel conteudo = new DashboardContentPanel();
        conteudo.setBackground(FUNDO_CATEGORIAS);
        conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        adicionarBloco(conteudo, criarCabecalho());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarResumo());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarFiltros());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarConteudo());

        JScrollPane scrollPane = new JScrollPane(conteudo);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(FUNDO_CATEGORIAS);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(FUNDO_CATEGORIAS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private void adicionarBloco(JPanel container, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, componente.getPreferredSize().height));
        container.add(componente);
    }

    private void configurarCardsResumo() {
        totalCategoriasCard.definirCorDestaque(AZUL_DESTAQUE);
        receitasCard.definirCorDestaque(VERDE_DESTAQUE);
        despesasCard.definirCorDestaque(VERMELHO_DESTAQUE);
        configurarTamanhoCardResumo(totalCategoriasCard);
        configurarTamanhoCardResumo(receitasCard);
        configurarTamanhoCardResumo(despesasCard);
        atualizarResumo(List.of());
    }

    private void configurarTamanhoCardResumo(DashboardSummaryCard card) {
        card.setPreferredSize(new Dimension(0, 154));
        card.setMinimumSize(new Dimension(180, 154));
    }

    private void configurarCamposFiltro() {
        filtroTipoComboBox.setPreferredSize(new Dimension(150, 38));
        filtroStatusComboBox.setPreferredSize(new Dimension(150, 38));
        pesquisaField.setPreferredSize(new Dimension(360, 38));
    }

    private void atualizarResumo(List<Categoria> categoriasExibidas) {
        List<Categoria> categoriasSeguras = categoriasExibidas != null ? categoriasExibidas : List.of();
        long receitas = categoriasSeguras.stream()
                .filter(categoria -> categoria.getTipo() == TipoCategoria.RECEITA)
                .count();
        long despesas = categoriasSeguras.stream()
                .filter(categoria -> categoria.getTipo() == TipoCategoria.DESPESA)
                .count();
        long ativas = categoriasSeguras.stream().filter(Categoria::isAtivo).count();

        totalCategoriasCard.atualizar(
                String.valueOf(categoriasSeguras.size()),
                ativas + " categoria(s) ativa(s)",
                categoriasOriginais.size() + " categoria(s) cadastrada(s)",
                UiStyles.TEXT_PRIMARY
        );
        receitasCard.atualizar(
                String.valueOf(receitas),
                "Usadas para entradas financeiras.",
                "Dentro do filtro atual.",
                UiStyles.SUCCESS
        );
        despesasCard.atualizar(
                String.valueOf(despesas),
                "Usadas para saidas financeiras.",
                "Dentro do filtro atual.",
                UiStyles.ERROR
        );
    }

    private void executarNovaCategoria() {
        if (novaCategoriaAction != null) {
            novaCategoriaAction.run();
        }
    }

    private void abrirFormulario(String titulo, DadosCategoriaForm dadosIniciais, Consumer<DadosCategoriaForm> aoSalvar) {
        fecharFormulario();
        CategoriaFormDialog dialog = new CategoriaFormDialog(
                SwingUtilities.getWindowAncestor(this),
                titulo,
                dadosIniciais,
                aoSalvar
        );
        formularioAtual = dialog;
        dialog.abrir();
        if (formularioAtual == dialog && !dialog.isDisplayable()) {
            formularioAtual = null;
        }
    }

    private static final class DashboardContentPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 40, 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
