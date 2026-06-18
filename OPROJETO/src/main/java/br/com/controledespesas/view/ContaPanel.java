package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.view.component.EmptyStatePanel;
import br.com.controledespesas.view.component.LoadingPanel;
import br.com.controledespesas.view.contract.ContaView;
import br.com.controledespesas.view.contract.DadosContaForm;

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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ContaPanel extends JPanel implements ContaView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";
    private static final int COLUMN_ACTIONS = 6;
    private static final Color FUNDO_CONTAS = new Color(0xF5F7FB);
    private static final Color FUNDO_DESTAQUE = new Color(0xEEF4FF);
    private static final Color AZUL_DESTAQUE = new Color(0x2F6FED);
    private static final Color VERDE_DESTAQUE = new Color(0x15803D);
    private static final Color LARANJA_DESTAQUE = new Color(0xEA580C);

    private final JButton novaContaButton;
    private final JButton filtrarButton;
    private final JButton limparFiltrosButton;
    private final JComboBox<String> filtroTipoComboBox;
    private final JComboBox<String> filtroStatusComboBox;
    private final JComboBox<CampoPesquisaConta> campoPesquisaComboBox;
    private final JComboBox<OrdenacaoConta> ordenacaoComboBox;
    private final JTextField pesquisaField;
    private final JLabel mensagemLabel;
    private final DashboardSummaryCard saldoTotalCard;
    private final DashboardSummaryCard contasAtivasCard;
    private final DashboardSummaryCard contasInativasCard;
    private final ContaTableModel tableModel;
    private final JTable tabela;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final EmptyStatePanel emptyStatePanel;
    private final MoneyFormatter moneyFormatter;
    private final ContaListSupport contaListSupport;

    private final List<Conta> contasOriginais = new ArrayList<>();
    private final Map<Long, BigDecimal> saldos = new HashMap<>();
    private ContaFormDialog formularioAtual;

    private Runnable novaContaAction;
    private Consumer<Conta> editarAction;
    private Consumer<Conta> alterarStatusAction;
    private Consumer<Conta> excluirAction;
    private boolean carregando;

    public ContaPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(FUNDO_CONTAS);

        moneyFormatter = new MoneyFormatter();
        novaContaButton = new JButton("Nova conta");
        filtrarButton = new JButton("Filtrar");
        limparFiltrosButton = new JButton("Limpar filtros");
        filtroTipoComboBox = new JComboBox<>(new String[]{
                "Todos", "Carteira", "Conta-corrente", "Poupanca", "Conta digital", "Outro"
        });
        filtroStatusComboBox = new JComboBox<>(new String[]{"Todas", "Ativas", "Inativas"});
        campoPesquisaComboBox = new JComboBox<>(CampoPesquisaConta.values());
        ordenacaoComboBox = new JComboBox<>(OrdenacaoConta.values());
        pesquisaField = new JTextField();
        mensagemLabel = UiStyles.createMessageLabel();
        saldoTotalCard = new DashboardSummaryCard("Saldo exibido");
        contasAtivasCard = new DashboardSummaryCard("Contas ativas");
        contasInativasCard = new DashboardSummaryCard("Contas inativas");
        tableModel = new ContaTableModel(moneyFormatter);
        tabela = new JTable(tableModel);
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        emptyStatePanel = new EmptyStatePanel(
                "Nenhuma conta cadastrada.",
                "Cadastre uma conta, carteira ou poupanca para organizar seu dinheiro.",
                "Nova conta"
        );
        contaListSupport = new ContaListSupport();

        UiStyles.stylePrimaryButton(novaContaButton);
        UiStyles.stylePrimaryButton(filtrarButton);
        UiStyles.styleSecondaryButton(limparFiltrosButton);
        UiStyles.styleTextComponent(pesquisaField);
        UiStyles.styleComboBox(filtroTipoComboBox);
        UiStyles.styleComboBox(filtroStatusComboBox);
        UiStyles.styleComboBox(campoPesquisaComboBox);
        UiStyles.styleComboBox(ordenacaoComboBox);
        configurarCardsResumo();
        configurarCamposFiltro();
        configurarNomesComponentes();
        emptyStatePanel.setAcao(this::executarNovaConta);

        add(criarScrollPane(), BorderLayout.CENTER);
        configurarTabela();
        configurarFiltros();
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirContas(List<Conta> contas) {
        contasOriginais.clear();
        if (contas != null) {
            contasOriginais.addAll(contas);
        }
        aplicarFiltros();
    }

    @Override
    public void exibirSaldos(Map<Long, BigDecimal> saldos) {
        this.saldos.clear();
        if (saldos != null) {
            this.saldos.putAll(saldos);
        }
        aplicarFiltros();
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        this.carregando = carregando;
        novaContaButton.setEnabled(!carregando);
        filtrarButton.setEnabled(!carregando);
        limparFiltrosButton.setEnabled(!carregando);
        filtroTipoComboBox.setEnabled(!carregando);
        filtroStatusComboBox.setEnabled(!carregando);
        campoPesquisaComboBox.setEnabled(!carregando);
        ordenacaoComboBox.setEnabled(!carregando);
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
        contasOriginais.clear();
        saldos.clear();
        tableModel.atualizarContas(List.of());
        tableModel.atualizarSaldos(Map.of());
        atualizarEstadoConteudo();
    }

    @Override
    public void abrirFormularioCadastro(Consumer<DadosContaForm> aoSalvar) {
        abrirFormulario("Nova conta", null, aoSalvar);
    }

    @Override
    public void abrirFormularioEdicao(Conta conta, Consumer<DadosContaForm> aoSalvar) {
        abrirFormulario(
                "Editar conta",
                new DadosContaForm(conta.getNome(), conta.getTipo(), conta.getInstituicao(), conta.getSaldoInicial()),
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
    public boolean confirmarExclusao(Conta conta) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja excluir a conta \"" + conta.getNome() + "\"?\nEssa acao nao podera ser desfeita.",
                "Confirmar exclusao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmarAlteracaoStatus(Conta conta, boolean novoStatus) {
        String acao = novoStatus ? "ativar" : "inativar";
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja " + acao + " a conta \"" + conta.getNome() + "\"?",
                "Confirmar alteracao de status",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public void definirAcaoNovaConta(Runnable acao) {
        this.novaContaAction = acao;
    }

    @Override
    public void definirAcaoEditar(Consumer<Conta> acao) {
        this.editarAction = acao;
    }

    @Override
    public void definirAcaoAlterarStatus(Consumer<Conta> acao) {
        this.alterarStatusAction = acao;
    }

    @Override
    public void definirAcaoExcluir(Consumer<Conta> acao) {
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

        JLabel titulo = new JLabel("Contas");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Acompanhe carteiras, contas bancarias e saldos em uma visao organizada.");
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

        JLabel observacao = new JLabel("Os saldos consideram o valor inicial da conta e as transacoes vinculadas.");
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
        wrapper.add(novaContaButton, BorderLayout.EAST);
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
        cards.add(saldoTotalCard, gbc);

        gbc.gridx = 1;
        cards.add(contasAtivasCard, gbc);

        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        cards.add(contasInativasCard, gbc);

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

        JLabel titulo = new JLabel("Filtros de contas");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Organize a lista por tipo, status, texto de busca e ordenacao.");
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
        filtros.add(criarLabeled("Tipo", filtroTipoComboBox), gbc);

        gbc.gridx = 1;
        filtros.add(criarLabeled("Status", filtroStatusComboBox), gbc);

        gbc.gridx = 2;
        filtros.add(criarLabeled("Pesquisar por", campoPesquisaComboBox), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filtros.add(criarLabeled("Termo", pesquisaField), gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        filtros.add(criarLabeled("Ordenar por", ordenacaoComboBox), gbc);

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
        gbc.gridwidth = 3;
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

        JLabel titulo = new JLabel("Lista de contas");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Saldos, status e acoes das contas cadastradas.");
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
                setBackground(isSelected ? new Color(0xE8F0FF) : UiStyles.WHITE);
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
                Conta conta = tableModel.getContaAt(row);
                mostrarMenuAcoes(conta, event.getComponent(), event.getX(), event.getY());
            }
        });

        novaContaButton.addActionListener(event -> executarNovaConta());
    }

    private void configurarFiltros() {
        filtroTipoComboBox.addActionListener(event -> aplicarFiltros());
        filtroStatusComboBox.addActionListener(event -> aplicarFiltros());
        campoPesquisaComboBox.addActionListener(event -> aplicarFiltros());
        ordenacaoComboBox.addActionListener(event -> aplicarFiltros());
        filtrarButton.addActionListener(event -> aplicarFiltros());
        limparFiltrosButton.addActionListener(event -> limparFiltros());
        pesquisaField.addActionListener(event -> aplicarFiltros());
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
        List<Conta> filtradas = contaListSupport.filtrarEOrdenar(
                contasOriginais,
                saldos,
                obterCampoPesquisaSelecionado(),
                pesquisaField.getText(),
                obterTipoSelecionado(),
                obterStatusSelecionado(),
                obterOrdenacaoSelecionada()
        );

        tableModel.atualizarContas(filtradas);
        tableModel.atualizarSaldos(saldos);
        atualizarResumo(filtradas);
        atualizarEstadoConteudo();
    }

    private TipoConta obterTipoSelecionado() {
        String filtro = (String) filtroTipoComboBox.getSelectedItem();
        if ("Carteira".equals(filtro)) {
            return TipoConta.CARTEIRA;
        }
        if ("Conta-corrente".equals(filtro)) {
            return TipoConta.CONTA_CORRENTE;
        }
        if ("Poupanca".equals(filtro)) {
            return TipoConta.POUPANCA;
        }
        if ("Conta digital".equals(filtro)) {
            return TipoConta.CONTA_DIGITAL;
        }
        if ("Outro".equals(filtro)) {
            return TipoConta.OUTRO;
        }
        return null;
    }

    private Boolean obterStatusSelecionado() {
        String filtro = (String) filtroStatusComboBox.getSelectedItem();
        if ("Ativas".equals(filtro)) {
            return Boolean.TRUE;
        }
        if ("Inativas".equals(filtro)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private CampoPesquisaConta obterCampoPesquisaSelecionado() {
        CampoPesquisaConta campo = (CampoPesquisaConta) campoPesquisaComboBox.getSelectedItem();
        return campo != null ? campo : CampoPesquisaConta.NOME;
    }

    private OrdenacaoConta obterOrdenacaoSelecionada() {
        OrdenacaoConta ordenacao = (OrdenacaoConta) ordenacaoComboBox.getSelectedItem();
        return ordenacao != null ? ordenacao : OrdenacaoConta.NOME_CRESCENTE;
    }

    private void limparFiltros() {
        if (campoPesquisaComboBox.getItemCount() > 0) {
            campoPesquisaComboBox.setSelectedItem(CampoPesquisaConta.NOME);
        }
        pesquisaField.setText("");
        if (filtroTipoComboBox.getItemCount() > 0) {
            filtroTipoComboBox.setSelectedIndex(0);
        }
        if (filtroStatusComboBox.getItemCount() > 0) {
            filtroStatusComboBox.setSelectedIndex(0);
        }
        if (ordenacaoComboBox.getItemCount() > 0) {
            ordenacaoComboBox.setSelectedItem(OrdenacaoConta.NOME_CRESCENTE);
        }
        aplicarFiltros();
    }

    private void mostrarMenuAcoes(Conta conta, Component component, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();

        JButton editarButton = criarMenuButton("Editar", () -> {
            if (editarAction != null) {
                editarAction.accept(conta);
            }
        });

        String textoStatus = conta.isAtivo() ? "Inativar" : "Ativar";
        JButton statusButton = criarMenuButton(textoStatus, () -> {
            if (alterarStatusAction != null) {
                alterarStatusAction.accept(conta);
            }
        });

        JButton excluirButton = criarMenuButton("Excluir", () -> {
            if (excluirAction != null) {
                excluirAction.accept(conta);
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
            if (contasOriginais.isEmpty()) {
                emptyStatePanel.setConteudo(
                        "Nenhuma conta cadastrada.",
                        "Cadastre uma conta, carteira ou poupanca para organizar seu dinheiro."
                );
            } else {
                emptyStatePanel.setConteudo(
                        "Nenhuma conta encontrada.",
                        "Ajuste os filtros ou cadastre uma nova conta."
                );
            }
            contentLayout.show(contentPanel, CARD_VAZIO);
            return;
        }

        contentLayout.show(contentPanel, CARD_LISTA);
    }

    private JScrollPane criarScrollPane() {
        DashboardContentPanel conteudo = new DashboardContentPanel();
        conteudo.setBackground(FUNDO_CONTAS);
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
        scrollPane.setBackground(FUNDO_CONTAS);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(FUNDO_CONTAS);
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
        saldoTotalCard.definirCorDestaque(AZUL_DESTAQUE);
        contasAtivasCard.definirCorDestaque(VERDE_DESTAQUE);
        contasInativasCard.definirCorDestaque(LARANJA_DESTAQUE);
        configurarTamanhoCardResumo(saldoTotalCard);
        configurarTamanhoCardResumo(contasAtivasCard);
        configurarTamanhoCardResumo(contasInativasCard);
        atualizarResumo(List.of());
    }

    private void configurarTamanhoCardResumo(DashboardSummaryCard card) {
        card.setPreferredSize(new Dimension(0, 154));
        card.setMinimumSize(new Dimension(180, 154));
    }

    private void configurarCamposFiltro() {
        filtroTipoComboBox.setPreferredSize(new Dimension(160, 38));
        filtroStatusComboBox.setPreferredSize(new Dimension(140, 38));
        campoPesquisaComboBox.setPreferredSize(new Dimension(170, 38));
        ordenacaoComboBox.setPreferredSize(new Dimension(210, 38));
        pesquisaField.setPreferredSize(new Dimension(330, 38));
    }

    private void atualizarResumo(List<Conta> contasExibidas) {
        List<Conta> contasSeguras = contasExibidas != null ? contasExibidas : List.of();
        BigDecimal saldoTotal = contasSeguras.stream()
                .map(conta -> saldos.getOrDefault(conta.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long ativas = contasSeguras.stream().filter(Conta::isAtivo).count();
        long inativas = contasSeguras.size() - ativas;

        saldoTotalCard.atualizar(
                moneyFormatter.format(saldoTotal),
                contasSeguras.size() + " conta(s) exibida(s)",
                contasOriginais.size() + " conta(s) cadastrada(s)",
                saldoTotal.signum() >= 0 ? UiStyles.SUCCESS : UiStyles.ERROR
        );
        contasAtivasCard.atualizar(
                String.valueOf(ativas),
                "Disponiveis para novas transacoes.",
                "Dentro do filtro atual.",
                UiStyles.SUCCESS
        );
        contasInativasCard.atualizar(
                String.valueOf(inativas),
                "Mantidas no historico financeiro.",
                "Dentro do filtro atual.",
                UiStyles.TEXT_PRIMARY
        );
    }

    private void executarNovaConta() {
        if (novaContaAction != null) {
            novaContaAction.run();
        }
    }

    private void configurarNomesComponentes() {
        novaContaButton.setName("novaContaButton");
        filtrarButton.setName("filtrarContasButton");
        limparFiltrosButton.setName("limparFiltrosContasButton");
        filtroTipoComboBox.setName("filtroTipoContaComboBox");
        filtroStatusComboBox.setName("filtroStatusContaComboBox");
        campoPesquisaComboBox.setName("campoPesquisaContaComboBox");
        ordenacaoComboBox.setName("ordenacaoContaComboBox");
        pesquisaField.setName("termoPesquisaContaField");
        tabela.setName("contasTable");
    }

    private void abrirFormulario(String titulo, DadosContaForm dadosIniciais, Consumer<DadosContaForm> aoSalvar) {
        fecharFormulario();
        ContaFormDialog dialog = new ContaFormDialog(
                SwingUtilities.getWindowAncestor(this),
                titulo,
                dadosIniciais,
                moneyFormatter,
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
