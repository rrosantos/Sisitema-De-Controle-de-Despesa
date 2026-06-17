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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

    private final JButton novaContaButton;
    private final JButton filtrarButton;
    private final JButton limparFiltrosButton;
    private final JComboBox<String> filtroTipoComboBox;
    private final JComboBox<String> filtroStatusComboBox;
    private final JComboBox<CampoPesquisaConta> campoPesquisaComboBox;
    private final JComboBox<OrdenacaoConta> ordenacaoComboBox;
    private final JTextField pesquisaField;
    private final JLabel mensagemLabel;
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
        setLayout(new BorderLayout(18, 18));
        setOpaque(false);

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
        configurarNomesComponentes();
        emptyStatePanel.setAcao(this::executarNovaConta);

        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarConteudo(), BorderLayout.CENTER);
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
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Contas");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Cadastre carteiras e contas para acompanhar saldo inicial e saldo atual.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitulo);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.WEST);
        top.add(novaContaButton, BorderLayout.EAST);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(criarFiltros(), BorderLayout.CENTER);

        mensagemLabel.setForeground(UiStyles.TEXT_SECONDARY);
        wrapper.add(mensagemLabel, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel criarFiltros() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);

        campoPesquisaComboBox.setPreferredSize(new Dimension(150, 40));
        pesquisaField.setPreferredSize(new Dimension(240, 40));
        ordenacaoComboBox.setPreferredSize(new Dimension(190, 40));

        filtros.add(criarLabeled("Tipo", filtroTipoComboBox));
        filtros.add(criarLabeled("Status", filtroStatusComboBox));
        filtros.add(criarLabeled("Pesquisar por", campoPesquisaComboBox));
        filtros.add(criarLabeled("Termo", pesquisaField));
        filtros.add(criarLabeled("Ordenar por", ordenacaoComboBox));
        filtros.add(filtrarButton);
        filtros.add(limparFiltrosButton);
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
        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(tabela);
        UiStyles.styleTableScrollPane(scrollPane);
        tabelaPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(tabelaPanel, CARD_LISTA);
        contentPanel.add(emptyStatePanel, CARD_VAZIO);
        contentPanel.add(new LoadingPanel(), CARD_LOADING);
        return contentPanel;
    }

    private void configurarTabela() {
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowSelectionAllowed(true);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(34);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setFont(UiStyles.TEXT_FONT);
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        UiStyles.styleTable(tabela);

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
}
