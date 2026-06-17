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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
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
    private final JLabel totalReceitasLabel;
    private final JLabel totalDespesasLabel;
    private final JLabel saldoPeriodoLabel;
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
        setLayout(new BorderLayout(18, 18));
        setOpaque(false);

        moneyFormatter = new MoneyFormatter();
        novaTransacaoButton = new JButton("Nova transacao");
        filtrarButton = new JButton("Filtrar");
        limparFiltrosButton = new JButton("Limpar filtros");
        dataInicialField = new JTextField();
        dataFinalField = new JTextField();
        tipoComboBox = new JComboBox<>();
        statusComboBox = new JComboBox<>();
        categoriaComboBox = new JComboBox<>();
        contaComboBox = new JComboBox<>();
        descricaoField = new JTextField();
        mensagemLabel = UiStyles.createMessageLabel();
        totalReceitasLabel = criarResumoValor();
        totalDespesasLabel = criarResumoValor();
        saldoPeriodoLabel = criarResumoValor();
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
        emptyStatePanel.setAcao(this::executarNovaTransacao);

        preencherCombosFixos();
        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarConteudo(), BorderLayout.CENTER);
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
        totalReceitasLabel.setText(moneyFormatter.format(totalReceitas != null ? totalReceitas : BigDecimal.ZERO));
        totalDespesasLabel.setText(moneyFormatter.format(totalDespesas != null ? totalDespesas : BigDecimal.ZERO));
        saldoPeriodoLabel.setText(moneyFormatter.format(saldoPeriodo != null ? saldoPeriodo : BigDecimal.ZERO));
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

        java.time.LocalDate dataInicial = DateFormatter.parse(dataInicialField.getText());
        java.time.LocalDate dataFinal = DateFormatter.parse(dataFinalField.getText());
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
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Transacoes");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Registre receitas e despesas com filtros, resumo e historico por periodo.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitulo);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.WEST);
        top.add(novaTransacaoButton, BorderLayout.EAST);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(criarResumo(), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(0, 12));
        inferior.setOpaque(false);
        inferior.add(criarFiltros(), BorderLayout.NORTH);
        inferior.add(mensagemLabel, BorderLayout.SOUTH);
        wrapper.add(inferior, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel criarResumo() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        cards.setOpaque(false);
        cards.add(criarCardResumo("Receitas recebidas", totalReceitasLabel));
        cards.add(criarCardResumo("Despesas pagas", totalDespesasLabel));
        cards.add(criarCardResumo("Saldo do periodo", saldoPeriodoLabel));

        JLabel aviso = new JLabel("Os cards consideram apenas o intervalo de datas informado.");
        aviso.setFont(UiStyles.SMALL_FONT);
        aviso.setForeground(UiStyles.TEXT_SECONDARY);

        wrapper.add(cards, BorderLayout.CENTER);
        wrapper.add(aviso, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel criarCardResumo(String titulo, JLabel valorLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(UiStyles.BACKGROUND);
        card.setBorder(UiStyles.createCardBorder());

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(UiStyles.LABEL_FONT);
        tituloLabel.setForeground(UiStyles.TEXT_SECONDARY);

        card.add(tituloLabel, BorderLayout.NORTH);
        card.add(valorLabel, BorderLayout.CENTER);
        return card;
    }

    private JLabel criarResumoValor() {
        JLabel label = new JLabel(moneyFormatter.format(BigDecimal.ZERO));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        label.setForeground(UiStyles.TEXT_PRIMARY);
        return label;
    }

    private JPanel criarFiltros() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);

        dataInicialField.setPreferredSize(new Dimension(120, 40));
        dataFinalField.setPreferredSize(new Dimension(120, 40));
        descricaoField.setPreferredSize(new Dimension(200, 40));
        categoriaComboBox.setPreferredSize(new Dimension(180, 40));
        contaComboBox.setPreferredSize(new Dimension(180, 40));

        filtros.add(criarLabeled("Data inicial", dataInicialField));
        filtros.add(criarLabeled("Data final", dataFinalField));
        filtros.add(criarLabeled("Tipo", tipoComboBox));
        filtros.add(criarLabeled("Status", statusComboBox));
        filtros.add(criarLabeled("Categoria", categoriaComboBox));
        filtros.add(criarLabeled("Conta", contaComboBox));
        filtros.add(criarLabeled("Descricao", descricaoField));
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
        return !dataInicialField.getText().isBlank()
                || !dataFinalField.getText().isBlank()
                || (tipo != null && tipo.value() != null)
                || (status != null && status.value() != null)
                || (categoria != null && categoria.value() != null)
                || (conta != null && conta.value() != null)
                || !descricaoField.getText().isBlank();
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
}
