package br.com.controledespesas.view;

import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class MovimentacaoCofrinhoDialog extends JDialog {

    private final JTextField valorField;
    private final JTextField dataField;
    private final JTextArea observacaoArea;
    private final JLabel mensagemLabel;
    private final JButton cancelarButton;
    private final JButton salvarButton;
    private final MoneyFormatter moneyFormatter;
    private final Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar;

    private boolean processando;

    MovimentacaoCofrinhoDialog(Window owner, String titulo, CofrinhoResumo resumo,
                               Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar, MoneyFormatter moneyFormatter) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.aoSalvar = Objects.requireNonNull(aoSalvar, "aoSalvar nao pode ser nulo.");
        this.moneyFormatter = Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");
        valorField = new JTextField();
        dataField = new JTextField();
        observacaoArea = new JTextArea(5, 20);
        mensagemLabel = UiStyles.createMessageLabel();
        cancelarButton = new JButton("Cancelar");
        salvarButton = new JButton("Salvar");
        initialize(resumo);
    }

    private void initialize(CofrinhoResumo resumo) {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(criarConteudo(resumo));

        dataField.setText(DateFormatter.format(LocalDate.now()));

        getRootPane().setDefaultButton(salvarButton);
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
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(valorField::requestFocusInWindow);
    }

    private JPanel criarConteudo(CofrinhoResumo resumo) {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        UiStyles.styleTextComponent(valorField);
        UiStyles.styleTextComponent(dataField);

        observacaoArea.setFont(UiStyles.TEXT_FONT);
        observacaoArea.setBackground(UiStyles.WHITE);
        observacaoArea.setForeground(UiStyles.TEXT_PRIMARY);
        observacaoArea.setLineWrap(true);
        observacaoArea.setWrapStyleWord(true);
        observacaoArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane observacaoScrollPane = new JScrollPane(observacaoArea);
        observacaoScrollPane.setBorder(UiStyles.createInputBorder());

        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));

        JLabel nomeLabel = new JLabel("Meta: " + resumo.cofrinho().getNome());
        nomeLabel.setFont(UiStyles.LABEL_FONT);
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel valorAtualLabel = new JLabel(
                "Valor atual: " + moneyFormatter.format(resumo.valorAtual())
                        + " de " + moneyFormatter.format(resumo.cofrinho().getValorMeta())
        );
        valorAtualLabel.setFont(UiStyles.TEXT_FONT);
        valorAtualLabel.setForeground(UiStyles.TEXT_SECONDARY);

        topo.add(nomeLabel);
        topo.add(Box.createVerticalStrut(6));
        topo.add(valorAtualLabel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);

        form.add(criarCampo("Valor", valorField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Data", dataField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Observacao", observacaoScrollPane), constraints);
        constraints.gridy++;
        mensagemLabel.setForeground(UiStyles.ERROR);
        form.add(mensagemLabel, constraints);

        root.add(topo, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(criarRodape(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel criarCampo(String label, JComponent component) {
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

    private JPanel criarRodape() {
        UiStyles.styleSecondaryButton(cancelarButton);
        UiStyles.stylePrimaryButton(salvarButton);

        cancelarButton.addActionListener(event -> fecharSePermitido());
        salvarButton.addActionListener(event -> salvar());

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(cancelarButton);
        buttons.add(salvarButton);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(buttons, BorderLayout.EAST);
        return rodape;
    }

    private void salvar() {
        mensagemLabel.setText(" ");

        try {
            DadosMovimentacaoCofrinhoForm dados = CofrinhoFormSupport.criarDadosMovimentacao(
                    valorField.getText(),
                    dataField.getText(),
                    observacaoArea.getText(),
                    moneyFormatter
            );
            setProcessando(true);
            aoSalvar.accept(dados);
        } catch (RuntimeException exception) {
            exibirErro(exception.getMessage());
        }
    }

    void abrir() {
        setVisible(true);
    }

    void fechar() {
        setProcessando(false);
        dispose();
    }

    void exibirErro(String mensagem) {
        setProcessando(false);
        mensagemLabel.setForeground(UiStyles.ERROR);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    private void fecharSePermitido() {
        if (!processando) {
            dispose();
        }
    }

    private void setProcessando(boolean processando) {
        this.processando = processando;
        valorField.setEnabled(!processando);
        dataField.setEnabled(!processando);
        observacaoArea.setEnabled(!processando);
        cancelarButton.setEnabled(!processando);
        salvarButton.setEnabled(!processando);
    }
}
