package br.com.controledespesas.view;

import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
import br.com.controledespesas.view.component.EmptyStatePanel;
import br.com.controledespesas.view.component.LoadingPanel;
import br.com.controledespesas.view.contract.DadosTransacaoForm;
import br.com.controledespesas.view.contract.TransacaoView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.MaskFormatter;
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
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class TransacaoPanel extends JPanel implements TransacaoView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";
    private static final int COLUMN_ACTIONS = 7;
    private static final String MASCARA_DATA = "##/##/####";
    private static final Color FUNDO_TRANSACOES = new Color(0xF5F7FB);
    private static final Color FUNDO_DESTAQUE = new Color(0xEEF4FF);
    private static final Color AZUL_DESTAQUE = new Color(0x2F6FED);
    private static final Color VERDE_DESTAQUE = new Color(0x15803D);
    private static final Color VERMELHO_DESTAQUE = new Color(0xB91C1C);
    private static final Color ROXO_DESTAQUE = new Color(0xA855F7);

    private final MoneyFormatter moneyFormatter;
    private final JButton novaTransacaoButton;
    private final JButton filtrarButton;
    private final JButton limparFiltrosButton;
    private final JTextField dataInicialField;
    private final JTextField dataFinalField;
    private final JComboBox<SelectionOption<TipoTransacao>> tipoComboBox;
    private final JComboBox<SelectionOption<StatusTransacao>> statusComboBox;
    private final JComboBox<SelectionOption<Long>> categoriaComboBox;
    private final JComboBox<SelectionOption<Long>> contaComboBox;
    private final JTextField descricaoField;
    private final JLabel mensagemLabel;
    private final DashboardSummaryCard totalReceitasCard;
    private final DashboardSummaryCard totalDespesasCard;
    private final DashboardSummaryCard saldoPeriodoCard;
    private final TransacaoTableModel tableModel;
    private final JTable tabela;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final EmptyStatePanel emptyStatePanel;

    private final Map<Long, Categoria> categorias = new LinkedHashMap<>();
    private final Map<Long, Conta> contas = new LinkedHashMap<>();

    private Runnable novaTransacaoAction;
    private Runnable filtrarAction;
    private Runnable limparFiltrosAction;
    private Consumer<Transacao> editarAction;
    private Consumer<Transacao> excluirAction;
    private boolean carregando;
    private TransacaoFormDialog formularioAtual;

    public TransacaoPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(FUNDO_TRANSACOES);

        moneyFormatter = new MoneyFormatter();
        novaTransacaoButton = new JButton("Nova transacao");
        filtrarButton = new JButton("Filtrar");
        limparFiltrosButton = new JButton("Limpar filtros");
        dataInicialField = criarCampoData();
        dataFinalField = criarCampoData();
        tipoComboBox = new JComboBox<>();
        statusComboBox = new JComboBox<>();
        categoriaComboBox = new JComboBox<>();
        contaComboBox = new JComboBox<>();
        descricaoField = new JTextField();
        mensagemLabel = UiStyles.createMessageLabel();
        totalReceitasCard = new DashboardSummaryCard("Receitas recebidas");
        totalDespesasCard = new DashboardSummaryCard("Despesas pagas");
        saldoPeriodoCard = new DashboardSummaryCard("Saldo do periodo");
        tableModel = new TransacaoTableModel(moneyFormatter);
        tabela = new JTable(tableModel);
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        emptyStatePanel = new EmptyStatePanel(
                "Nenhuma transacao encontrada.",
                "Registre receitas e despesas para comecar a controlar suas financas.",
                "Nova transacao"
        );

        UiStyles.stylePrimaryButton(novaTransacaoButton);
        UiStyles.stylePrimaryButton(filtrarButton);
        UiStyles.styleSecondaryButton(limparFiltrosButton);
        UiStyles.styleTextComponent(dataInicialField);
        UiStyles.styleTextComponent(dataFinalField);
        UiStyles.styleTextComponent(descricaoField);
        UiStyles.styleComboBox(tipoComboBox);
        UiStyles.styleComboBox(statusComboBox);
        UiStyles.styleComboBox(categoriaComboBox);
        UiStyles.styleComboBox(contaComboBox);
        configurarCardsResumo();
        configurarCamposFiltro();
        emptyStatePanel.setAcao(this::executarNovaTransacao);

        preencherCombosFixos();
        add(criarScrollPane(), BorderLayout.CENTER);
        configurarTabela();
        configurarAcoesLocais();
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirTransacoes(List<Transacao> transacoes) {
        tableModel.atualizarTransacoes(transacoes);
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirDadosRelacionados(Map<Long, Categoria> categorias, Map<Long, Conta> contas) {
        SelectionOption<Long> categoriaSelecionada = obterSelecionado(categoriaComboBox);
        SelectionOption<Long> contaSelecionada = obterSelecionado(contaComboBox);
        Long categoriaIdSelecionada = categoriaSelecionada != null ? categoriaSelecionada.value() : null;
        Long contaIdSelecionada = contaSelecionada != null ? contaSelecionada.value() : null;

        this.categorias.clear();
        this.contas.clear();
        if (categorias != null) {
            this.categorias.putAll(categorias);
        }
        if (contas != null) {
            this.contas.putAll(contas);
        }

        tableModel.atualizarDadosRelacionados(this.categorias, this.contas);
        atualizarFiltroCategorias(categoriaIdSelecionada);
        atualizarFiltroContas(contaIdSelecionada);
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirResumo(BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldoPeriodo) {
        BigDecimal receitas = totalReceitas != null ? totalReceitas : BigDecimal.ZERO;
        BigDecimal despesas = totalDespesas != null ? totalDespesas : BigDecimal.ZERO;
        BigDecimal saldo = saldoPeriodo != null ? saldoPeriodo : BigDecimal.ZERO;

        totalReceitasCard.atualizar(
                moneyFormatter.format(receitas),
                "Entradas recebidas no periodo.",
                "Valores conforme filtros aplicados.",
                UiStyles.SUCCESS
        );
        totalDespesasCard.atualizar(
                moneyFormatter.format(despesas),
                "Saidas pagas no periodo.",
                "Pendentes e canceladas seguem no historico.",
                UiStyles.ERROR
        );
        saldoPeriodoCard.atualizar(
                moneyFormatter.format(saldo),
                "Receitas menos despesas.",
                tableModel.getRowCount() + " transacao(oes) listada(s).",
                saldo.signum() >= 0 ? UiStyles.SUCCESS : UiStyles.ERROR
        );
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        this.carregando = carregando;
        novaTransacaoButton.setEnabled(!carregando);
        filtrarButton.setEnabled(!carregando);
        limparFiltrosButton.setEnabled(!carregando);
        dataInicialField.setEnabled(!carregando);
        dataFinalField.setEnabled(!carregando);
        tipoComboBox.setEnabled(!carregando);
        statusComboBox.setEnabled(!carregando);
        categoriaComboBox.setEnabled(!carregando);
        contaComboBox.setEnabled(!carregando);
        descricaoField.setEnabled(!carregando);
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
        atualizarEstadoConteudo();
    }

    @Override
    public void abrirFormularioCadastro(List<Categoria> categorias, List<Conta> contas, Consumer<DadosTransacaoForm> aoSalvar) {
        abrirFormulario("Nova transacao", null, categorias, contas, aoSalvar);
    }

    @Override
    public void abrirFormularioEdicao(Transacao transacao, List<Categoria> categorias, List<Conta> contas,
                                      Consumer<DadosTransacaoForm> aoSalvar) {
        abrirFormulario("Editar transacao", transacao, categorias, contas, aoSalvar);
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
    public boolean confirmarExclusao(Transacao transacao) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja excluir a transacao \"" + transacao.getDescricao() + "\"?\n"
                        + "Data: " + DateFormatter.format(transacao.getDataTransacao()) + "\n"
                        + "Valor: " + moneyFormatter.format(transacao.getValor()) + "\n"
                        + "Tipo: " + ViewFormatters.formatTipoTransacao(transacao.getTipo()) + "\n\n"
                        + "Essa acao nao podera ser desfeita.",
                "Confirmar exclusao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public TransacaoFiltro obterFiltro() {
        SelectionOption<TipoTransacao> tipo = obterSelecionado(tipoComboBox);
        SelectionOption<StatusTransacao> status = obterSelecionado(statusComboBox);
        SelectionOption<Long> categoria = obterSelecionado(categoriaComboBox);
        SelectionOption<Long> conta = obterSelecionado(contaComboBox);

        java.time.LocalDate dataInicial = DateFormatter.parse(obterTextoData(dataInicialField));
        java.time.LocalDate dataFinal = DateFormatter.parse(obterTextoData(dataFinalField));
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new ValidacaoException("A data inicial nao pode ser posterior a data final.");
        }

        return new TransacaoFiltro(
                dataInicial,
                dataFinal,
                tipo != null ? tipo.value() : null,
                status != null ? status.value() : null,
                categoria != null ? categoria.value() : null,
                conta != null ? conta.value() : null,
                descricaoField.getText()
        );
    }

    @Override
    public void limparFiltros() {
        dataInicialField.setText("");
        dataFinalField.setText("");
        if (tipoComboBox.getItemCount() > 0) {
            tipoComboBox.setSelectedIndex(0);
        }
        if (statusComboBox.getItemCount() > 0) {
            statusComboBox.setSelectedIndex(0);
        }
        if (categoriaComboBox.getItemCount() > 0) {
            categoriaComboBox.setSelectedIndex(0);
        }
        if (contaComboBox.getItemCount() > 0) {
            contaComboBox.setSelectedIndex(0);
        }
        descricaoField.setText("");
    }

    @Override
    public void definirAcaoNovaTransacao(Runnable acao) {
        this.novaTransacaoAction = acao;
    }

    @Override
    public void definirAcaoFiltrar(Runnable acao) {
        this.filtrarAction = acao;
    }

    @Override
    public void definirAcaoLimparFiltros(Runnable acao) {
        this.limparFiltrosAction = acao;
    }

    @Override
    public void definirAcaoEditar(Consumer<Transacao> acao) {
        this.editarAction = acao;
    }

    @Override
    public void definirAcaoExcluir(Consumer<Transacao> acao) {
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

        JLabel titulo = new JLabel("Transacoes");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Registre, filtre e acompanhe receitas e despesas em um unico lugar.");
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

        JLabel observacao = new JLabel("Use os filtros para analisar periodos, contas, categorias e status especificos.");
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
        wrapper.add(novaTransacaoButton, BorderLayout.EAST);
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
        cards.add(totalReceitasCard, gbc);

        gbc.gridx = 1;
        cards.add(totalDespesasCard, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        cards.add(saldoPeriodoCard, gbc);

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

        JLabel titulo = new JLabel("Filtros de busca");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Refine a lista por periodo, tipo, status, categoria, conta ou descricao.");
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
        gbc.insets = new Insets(0, 0, 12, 12);

        gbc.gridx = 0;
        filtros.add(criarLabeled("Data inicial", dataInicialField), gbc);

        gbc.gridx = 1;
        filtros.add(criarLabeled("Data final", dataFinalField), gbc);

        gbc.gridx = 2;
        filtros.add(criarLabeled("Tipo", tipoComboBox), gbc);

        gbc.gridx = 3;
        filtros.add(criarLabeled("Status", statusComboBox), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        filtros.add(criarLabeled("Categoria", categoriaComboBox), gbc);

        gbc.gridx = 1;
        filtros.add(criarLabeled("Conta", contaComboBox), gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filtros.add(criarLabeled("Descricao", descricaoField), gbc);

        JPanel acoes = new JPanel(new GridBagLayout());
        acoes.setBackground(new Color(0xF9FBFF));
        acoes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints acaoGbc = new GridBagConstraints();
        acaoGbc.gridy = 0;
        acaoGbc.anchor = GridBagConstraints.WEST;
        acaoGbc.insets = new Insets(0, 0, 0, 10);
        acaoGbc.gridx = 0;
        acoes.add(filtrarButton, acaoGbc);
        acaoGbc.gridx = 1;
        acaoGbc.insets = new Insets(0, 0, 0, 0);
        acoes.add(limparFiltrosButton, acaoGbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 0, 0);
        filtros.add(acoes, gbc);

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

        JLabel titulo = new JLabel("Historico de transacoes");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Clique em Acoes para editar ou excluir uma transacao.");
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
                Transacao transacao = tableModel.getTransacaoAt(row);
                mostrarMenuAcoes(transacao, event.getComponent(), event.getX(), event.getY());
            }
        });
    }

    private void configurarAcoesLocais() {
        novaTransacaoButton.addActionListener(event -> executarNovaTransacao());
        filtrarButton.addActionListener(event -> executarFiltro());
        limparFiltrosButton.addActionListener(event -> executarLimpezaFiltros());
        descricaoField.addActionListener(event -> executarFiltro());
    }

    private void preencherCombosFixos() {
        tipoComboBox.removeAllItems();
        tipoComboBox.addItem(new SelectionOption<>(null, "Todos"));
        tipoComboBox.addItem(new SelectionOption<>(TipoTransacao.RECEITA, "Receita"));
        tipoComboBox.addItem(new SelectionOption<>(TipoTransacao.DESPESA, "Despesa"));

        statusComboBox.removeAllItems();
        statusComboBox.addItem(new SelectionOption<>(null, "Todos"));
        statusComboBox.addItem(new SelectionOption<>(StatusTransacao.PENDENTE, "Pendente"));
        statusComboBox.addItem(new SelectionOption<>(StatusTransacao.PAGO, "Pago"));
        statusComboBox.addItem(new SelectionOption<>(StatusTransacao.RECEBIDO, "Recebido"));
        statusComboBox.addItem(new SelectionOption<>(StatusTransacao.CANCELADO, "Cancelado"));

        atualizarFiltroCategorias(null);
        atualizarFiltroContas(null);
    }

    private void atualizarFiltroCategorias(Long categoriaSelecionadaId) {
        categoriaComboBox.removeAllItems();
        categoriaComboBox.addItem(new SelectionOption<>(null, "Todas"));
        categorias.values().stream()
                .sorted(Comparator.comparing(Categoria::getNome, String.CASE_INSENSITIVE_ORDER))
                .forEach(categoria -> categoriaComboBox.addItem(new SelectionOption<>(categoria.getId(), categoria.getNome())));
        selecionarOpcaoPorValor(categoriaComboBox, categoriaSelecionadaId);
    }

    private void atualizarFiltroContas(Long contaSelecionadaId) {
        contaComboBox.removeAllItems();
        contaComboBox.addItem(new SelectionOption<>(null, "Todas"));
        contas.values().stream()
                .sorted(Comparator.comparing(Conta::getNome, String.CASE_INSENSITIVE_ORDER))
                .forEach(conta -> contaComboBox.addItem(new SelectionOption<>(conta.getId(), conta.getNome())));
        selecionarOpcaoPorValor(contaComboBox, contaSelecionadaId);
    }

    private <T> void selecionarOpcaoPorValor(JComboBox<SelectionOption<T>> comboBox, T valor) {
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            SelectionOption<T> option = comboBox.getItemAt(index);
            if (option != null && Objects.equals(option.value(), valor)) {
                comboBox.setSelectedIndex(index);
                return;
            }
        }
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }

    private <T> SelectionOption<T> obterSelecionado(JComboBox<SelectionOption<T>> comboBox) {
        int index = comboBox.getSelectedIndex();
        return index >= 0 ? comboBox.getItemAt(index) : null;
    }

    private void mostrarMenuAcoes(Transacao transacao, Component component, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();

        JButton editarButton = criarMenuButton("Editar", () -> {
            if (editarAction != null) {
                editarAction.accept(transacao);
            }
        });
        JButton excluirButton = criarMenuButton("Excluir", () -> {
            if (excluirAction != null) {
                excluirAction.accept(transacao);
            }
        });

        popupMenu.add(editarButton);
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
            if (possuiFiltrosVisuais()) {
                emptyStatePanel.setConteudo(
                        "Nenhuma transacao corresponde aos filtros informados.",
                        "Ajuste os filtros ou registre uma nova transacao."
                );
            } else {
                emptyStatePanel.setConteudo(
                        "Nenhuma transacao encontrada.",
                        "Registre receitas e despesas para comecar a controlar suas financas."
                );
            }
            contentLayout.show(contentPanel, CARD_VAZIO);
            return;
        }

        contentLayout.show(contentPanel, CARD_LISTA);
    }

    private boolean possuiFiltrosVisuais() {
        SelectionOption<TipoTransacao> tipo = obterSelecionado(tipoComboBox);
        SelectionOption<StatusTransacao> status = obterSelecionado(statusComboBox);
        SelectionOption<Long> categoria = obterSelecionado(categoriaComboBox);
        SelectionOption<Long> conta = obterSelecionado(contaComboBox);
        return !obterTextoData(dataInicialField).isBlank()
                || !obterTextoData(dataFinalField).isBlank()
                || (tipo != null && tipo.value() != null)
                || (status != null && status.value() != null)
                || (categoria != null && categoria.value() != null)
                || (conta != null && conta.value() != null)
                || !descricaoField.getText().isBlank();
    }

    private JScrollPane criarScrollPane() {
        DashboardContentPanel conteudo = new DashboardContentPanel();
        conteudo.setBackground(FUNDO_TRANSACOES);
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
        scrollPane.setBackground(FUNDO_TRANSACOES);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(FUNDO_TRANSACOES);
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
        totalReceitasCard.definirCorDestaque(VERDE_DESTAQUE);
        totalDespesasCard.definirCorDestaque(VERMELHO_DESTAQUE);
        saldoPeriodoCard.definirCorDestaque(ROXO_DESTAQUE);
        configurarTamanhoCardResumo(totalReceitasCard);
        configurarTamanhoCardResumo(totalDespesasCard);
        configurarTamanhoCardResumo(saldoPeriodoCard);
        exibirResumo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private void configurarTamanhoCardResumo(DashboardSummaryCard card) {
        card.setPreferredSize(new Dimension(0, 154));
        card.setMinimumSize(new Dimension(180, 154));
    }

    private void configurarCamposFiltro() {
        dataInicialField.setPreferredSize(new Dimension(122, 38));
        dataFinalField.setPreferredSize(new Dimension(122, 38));
        tipoComboBox.setPreferredSize(new Dimension(130, 38));
        statusComboBox.setPreferredSize(new Dimension(132, 38));
        categoriaComboBox.setPreferredSize(new Dimension(170, 38));
        contaComboBox.setPreferredSize(new Dimension(170, 38));
        descricaoField.setPreferredSize(new Dimension(260, 38));
        dataInicialField.setToolTipText("Informe a data no formato dd/MM/aaaa");
        dataFinalField.setToolTipText("Informe a data no formato dd/MM/aaaa");
    }

    private JTextField criarCampoData() {
        try {
            MaskFormatter formatter = new MaskFormatter(MASCARA_DATA);
            formatter.setPlaceholderCharacter('_');
            formatter.setAllowsInvalid(false);

            JFormattedTextField campo = new JFormattedTextField(formatter);
            campo.setColumns(10);
            campo.setFocusLostBehavior(JFormattedTextField.PERSIST);
            return campo;
        } catch (ParseException exception) {
            throw new IllegalStateException("Mascara de data invalida.", exception);
        }
    }

    private String obterTextoData(JTextField campo) {
        String texto = campo.getText();
        if (texto == null || texto.chars().noneMatch(Character::isDigit)) {
            return "";
        }
        return texto;
    }

    private void executarNovaTransacao() {
        if (novaTransacaoAction != null) {
            novaTransacaoAction.run();
        }
    }

    private void executarFiltro() {
        if (filtrarAction != null) {
            filtrarAction.run();
        }
    }

    private void executarLimpezaFiltros() {
        if (limparFiltrosAction != null) {
            limparFiltrosAction.run();
        }
    }

    private void abrirFormulario(String titulo, Transacao transacao, List<Categoria> categorias, List<Conta> contas,
                                 Consumer<DadosTransacaoForm> aoSalvar) {
        fecharFormulario();
        TransacaoFormDialog dialog = new TransacaoFormDialog(
                SwingUtilities.getWindowAncestor(this),
                titulo,
                transacao,
                new ArrayList<>(categorias),
                new ArrayList<>(contas),
                aoSalvar,
                moneyFormatter
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
