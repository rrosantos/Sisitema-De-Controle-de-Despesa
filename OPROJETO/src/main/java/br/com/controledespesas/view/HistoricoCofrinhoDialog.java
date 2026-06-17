package br.com.controledespesas.view;

import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.view.component.EmptyStatePanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;

public class HistoricoCofrinhoDialog extends JDialog {

    private static final String CARD_TABELA = "tabela";
    private static final String CARD_VAZIO = "vazio";

    private final JLabel nomeLabel;
    private final JLabel resumoLabel;
    private final JLabel percentualLabel;
    private final JLabel mensagemLabel;
    private final JButton excluirButton;
    private final JButton fecharButton;
    private final MoneyFormatter moneyFormatter;
    private final MovimentacaoCofrinhoTableModel tableModel;
    private final JTable tabela;
    private final JPanel contentPanel;
    private final CardLayout contentLayout;
    private final EmptyStatePanel emptyStatePanel;

    private Consumer<MovimentacaoCofrinho> aoExcluir;
    private boolean processando;

    HistoricoCofrinhoDialog(Window owner, CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes,
                            Consumer<MovimentacaoCofrinho> aoExcluir, MoneyFormatter moneyFormatter) {
        super(owner, "Historico do cofrinho", ModalityType.MODELESS);
        nomeLabel = new JLabel();
        resumoLabel = new JLabel();
        percentualLabel = new JLabel();
        mensagemLabel = UiStyles.createMessageLabel();
        excluirButton = new JButton("Excluir selecionada");
        fecharButton = new JButton("Fechar");
        this.moneyFormatter = moneyFormatter;
        tableModel = new MovimentacaoCofrinhoTableModel(moneyFormatter);
        tabela = new JTable(tableModel);
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        emptyStatePanel = new EmptyStatePanel(
                "Nenhuma movimentacao registrada neste cofrinho.",
                "Adicione depositos ou retiradas para acompanhar a evolucao da meta."
        );
        initialize();
        atualizar(resumo, movimentacoes, aoExcluir);
    }

    private void initialize() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(criarConteudo());
        getRootPane().registerKeyboardAction(
                event -> fecharSePermitido(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fecharSePermitido();
            }
        });

        pack();
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(getOwner());
    }

    private JPanel criarConteudo() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        nomeLabel.setFont(UiStyles.TITLE_FONT);
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        resumoLabel.setFont(UiStyles.TEXT_FONT);
        resumoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        percentualLabel.setFont(UiStyles.TEXT_FONT);
        percentualLabel.setForeground(UiStyles.TEXT_SECONDARY);

        mensagemLabel.setForeground(UiStyles.ERROR);

        header.add(nomeLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(resumoLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(percentualLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(mensagemLabel);

        JPanel tabelaPanel = new JPanel(new BorderLayout());
        tabelaPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(tabela);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        tabelaPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(tabelaPanel, CARD_TABELA);
        contentPanel.add(emptyStatePanel, CARD_VAZIO);

        configurarTabela();

        root.add(header, BorderLayout.NORTH);
        root.add(contentPanel, BorderLayout.CENTER);
        root.add(criarRodape(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel criarRodape() {
        UiStyles.styleSecondaryButton(excluirButton);
        UiStyles.styleSecondaryButton(fecharButton);

        excluirButton.addActionListener(event -> excluirSelecionada());
        fecharButton.addActionListener(event -> fecharSePermitido());

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(excluirButton);
        buttons.add(fecharButton);

        rodape.add(buttons, BorderLayout.EAST);
        return rodape;
    }

    private void configurarTabela() {
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowSelectionAllowed(true);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(34);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.setFont(UiStyles.TEXT_FONT);
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        tabela.getSelectionModel().addListSelectionListener(event -> atualizarEstadoExclusao());
    }

    void abrir() {
        setVisible(true);
        toFront();
    }

    void fechar() {
        setProcessando(false);
        dispose();
    }

    void atualizar(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes, Consumer<MovimentacaoCofrinho> aoExcluir) {
        this.aoExcluir = aoExcluir;
        atualizarResumo(resumo);
        tableModel.atualizarMovimentacoes(movimentacoes);
        mensagemLabel.setText(" ");
        atualizarEstadoConteudo();
        setProcessando(false);
    }

    void atualizar(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes) {
        atualizarResumo(resumo);
        tableModel.atualizarMovimentacoes(movimentacoes);
        mensagemLabel.setText(" ");
        atualizarEstadoConteudo();
        setProcessando(false);
    }

    void exibirErro(String mensagem) {
        setProcessando(false);
        mensagemLabel.setForeground(UiStyles.ERROR);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    private void atualizarResumo(CofrinhoResumo resumo) {
        nomeLabel.setText(resumo.cofrinho().getNome());
        resumoLabel.setText(
                moneyFormatter.format(resumo.valorAtual())
                        + " de " + moneyFormatter.format(resumo.cofrinho().getValorMeta())
        );
        percentualLabel.setText("Percentual: " + CofrinhoViewSupport.formatarPercentual(resumo.percentualProgresso()));
    }

    private void excluirSelecionada() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            exibirErro("Selecione uma movimentacao.");
            return;
        }

        if (aoExcluir != null) {
            setProcessando(true);
            aoExcluir.accept(tableModel.getMovimentacaoAt(row));
        }
    }

    private void atualizarEstadoConteudo() {
        if (tableModel.getRowCount() == 0) {
            contentLayout.show(contentPanel, CARD_VAZIO);
        } else {
            contentLayout.show(contentPanel, CARD_TABELA);
        }
        atualizarEstadoExclusao();
    }

    private void atualizarEstadoExclusao() {
        excluirButton.setEnabled(!processando && tabela.getSelectedRow() >= 0 && tableModel.getRowCount() > 0);
    }

    private void fecharSePermitido() {
        if (!processando) {
            dispose();
        }
    }

    private void setProcessando(boolean processando) {
        this.processando = processando;
        tabela.setEnabled(!processando);
        fecharButton.setEnabled(!processando);
        atualizarEstadoExclusao();
    }
}
