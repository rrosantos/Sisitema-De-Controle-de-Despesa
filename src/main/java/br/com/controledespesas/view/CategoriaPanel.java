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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CategoriaPanel extends JPanel implements CategoriaView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";
    private static final int COLUMN_ACTIONS = 4;

    private final JButton novaCategoriaButton;
    private final JComboBox<String> filtroTipoComboBox;
    private final JComboBox<String> filtroStatusComboBox;
    private final JTextField pesquisaField;
    private final JLabel mensagemLabel;
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
        setLayout(new BorderLayout(18, 18));
        setOpaque(false);

        novaCategoriaButton = new JButton("Nova categoria");
        filtroTipoComboBox = new JComboBox<>(new String[]{"Todas", "Receita", "Despesa"});
        filtroStatusComboBox = new JComboBox<>(new String[]{"Todas", "Ativas", "Inativas"});
        pesquisaField = new JTextField();
        mensagemLabel = UiStyles.createMessageLabel();
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
        emptyStatePanel.setAcao(this::executarNovaCategoria);

        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarConteudo(), BorderLayout.CENTER);
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
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Categorias");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Gerencie categorias de receita e despesa para organizar seus lancamentos.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitulo);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(novaCategoriaButton, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);

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
        filtros.add(criarLabeled("Pesquisar por nome", pesquisaField));
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
}
