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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ContaPanel extends JPanel implements ContaView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";
    private static final int COLUMN_ACTIONS = 6;

    private final JButton novaContaButton;
    private final JComboBox<String> filtroTipoComboBox;
    private final JComboBox<String> filtroStatusComboBox;
    private final JTextField pesquisaField;
    private final JLabel mensagemLabel;
    private final ContaTableModel tableModel;
    private final JTable tabela;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final EmptyStatePanel emptyStatePanel;
    private final MoneyFormatter moneyFormatter;

    private final List<Conta> contasOriginais = new ArrayList<>();
    private final Map<Long, BigDecimal> saldos = new HashMap<>();

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
        filtroTipoComboBox = new JComboBox<>(new String[]{
                "Todos", "Carteira", "Conta-corrente", "Poupanca", "Conta digital", "Outro"
        });
        filtroStatusComboBox = new JComboBox<>(new String[]{"Todas", "Ativas", "Inativas"});
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

        UiStyles.stylePrimaryButton(novaContaButton);
        UiStyles.styleTextComponent(pesquisaField);
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
        tableModel.atualizarSaldos(this.saldos);
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        this.carregando = carregando;
        novaContaButton.setEnabled(!carregando);
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
        contasOriginais.clear();
        saldos.clear();
        tableModel.atualizarContas(List.of());
        tableModel.atualizarSaldos(Map.of());
        atualizarEstadoConteudo();
    }

    @Override
    public Optional<DadosContaForm> abrirFormularioCadastro() {
        return ContaFormDialog.showDialog(
                SwingUtilities.getWindowAncestor(this),
                "Nova conta",
                null,
                moneyFormatter
        );
    }

    @Override
    public Optional<DadosContaForm> abrirFormularioEdicao(Conta conta) {
        return ContaFormDialog.showDialog(
                SwingUtilities.getWindowAncestor(this),
                "Editar conta",
                new DadosContaForm(conta.getNome(), conta.getTipo(), conta.getInstituicao(), conta.getSaldoInicial()),
                moneyFormatter
        );
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

        pesquisaField.setPreferredSize(new Dimension(220, 40));

        filtros.add(criarLabeled("Tipo", filtroTipoComboBox));
        filtros.add(criarLabeled("Status", filtroStatusComboBox));
        filtros.add(criarLabeled("Pesquisar por nome ou instituicao", pesquisaField));
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
        scrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
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

        DefaultTableCellRenderer buttonRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText("Acoes");
                setHorizontalAlignment(CENTER);
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
        List<Conta> filtradas = contasOriginais.stream()
                .filter(this::filtrarPorTipo)
                .filter(this::filtrarPorStatus)
                .filter(this::filtrarPorPesquisa)
                .toList();

        tableModel.atualizarContas(filtradas);
        tableModel.atualizarSaldos(saldos);
        atualizarEstadoConteudo();
    }

    private boolean filtrarPorTipo(Conta conta) {
        String filtro = (String) filtroTipoComboBox.getSelectedItem();
        if ("Carteira".equals(filtro)) {
            return conta.getTipo() == TipoConta.CARTEIRA;
        }
        if ("Conta-corrente".equals(filtro)) {
            return conta.getTipo() == TipoConta.CONTA_CORRENTE;
        }
        if ("Poupanca".equals(filtro)) {
            return conta.getTipo() == TipoConta.POUPANCA;
        }
        if ("Conta digital".equals(filtro)) {
            return conta.getTipo() == TipoConta.CONTA_DIGITAL;
        }
        if ("Outro".equals(filtro)) {
            return conta.getTipo() == TipoConta.OUTRO;
        }
        return true;
    }

    private boolean filtrarPorStatus(Conta conta) {
        String filtro = (String) filtroStatusComboBox.getSelectedItem();
        if ("Ativas".equals(filtro)) {
            return conta.isAtivo();
        }
        if ("Inativas".equals(filtro)) {
            return !conta.isAtivo();
        }
        return true;
    }

    private boolean filtrarPorPesquisa(Conta conta) {
        String termo = pesquisaField.getText();
        if (termo == null || termo.isBlank()) {
            return true;
        }

        String termoNormalizado = termo.trim().toLowerCase(Locale.ROOT);
        return conta.getNome().toLowerCase(Locale.ROOT).contains(termoNormalizado)
                || (conta.getInstituicao() != null
                && conta.getInstituicao().toLowerCase(Locale.ROOT).contains(termoNormalizado));
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
}
