package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.view.contract.DadosContaForm;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Exibe e controla o dialogo Swing de ContaForm.
 */
public class ContaFormDialog extends JDialog {

    private final JTextField nomeField;
    private final JComboBox<TipoConta> tipoComboBox;
    private final JTextField instituicaoField;
    private final JTextField saldoInicialField;
    private final JLabel mensagemLabel;
    private final MoneyFormatter moneyFormatter;
    private final JButton cancelarButton;
    private final JButton salvarButton;
    private final Consumer<DadosContaForm> aoSalvar;

    private boolean processando;

    ContaFormDialog(Window owner, String titulo, DadosContaForm dadosIniciais, MoneyFormatter moneyFormatter,
                    Consumer<DadosContaForm> aoSalvar) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.moneyFormatter = Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");
        this.aoSalvar = Objects.requireNonNull(aoSalvar, "aoSalvar nao pode ser nulo.");
        nomeField = new JTextField();
        tipoComboBox = new JComboBox<>(TipoConta.values());
        instituicaoField = new JTextField();
        saldoInicialField = new JTextField();
        InputFormatters.instalarFormatoMonetario(saldoInicialField, this.moneyFormatter);
        mensagemLabel = UiStyles.createMessageLabel();
        cancelarButton = new JButton("Cancelar");
        salvarButton = new JButton("Salvar");
        initialize(dadosIniciais);
    }

    private void initialize(DadosContaForm dadosIniciais) {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(criarConteudo());
        preencherDados(dadosIniciais);

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
        setMinimumSize(new Dimension(430, 330));
        setLocationRelativeTo(getOwner());
        javax.swing.SwingUtilities.invokeLater(nomeField::requestFocusInWindow);
    }

    private JPanel criarConteudo() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        UiStyles.styleTextComponent(nomeField);
        UiStyles.styleTextComponent(instituicaoField);
        UiStyles.styleTextComponent(saldoInicialField);
        UiStyles.styleComboBox(tipoComboBox);

        tipoComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(ViewFormatters.formatTipoConta(value));
            boolean popupItemSelected = isSelected && index >= 0;
            label.setOpaque(true);
            label.setFont(UiStyles.TEXT_FONT);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
            label.setBackground(popupItemSelected ? new java.awt.Color(0xE8F0FF) : UiStyles.WHITE);
            label.setForeground(popupItemSelected ? UiStyles.PRIMARY_DARK : UiStyles.TEXT_PRIMARY);
            return label;
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);

        form.add(criarCampo("Nome", nomeField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Tipo", tipoComboBox), constraints);
        constraints.gridy++;
        form.add(criarCampo("Instituicao", instituicaoField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Saldo inicial", saldoInicialField), constraints);
        constraints.gridy++;
        mensagemLabel.setForeground(UiStyles.ERROR);
        form.add(mensagemLabel, constraints);

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
        getRootPane().setDefaultButton(salvarButton);

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
        String nome = nomeField.getText() != null ? nomeField.getText().trim() : "";
        TipoConta tipo = (TipoConta) tipoComboBox.getSelectedItem();
        String instituicao = instituicaoField.getText();
        String saldoTexto = saldoInicialField.getText();

        if (nome.isBlank()) {
            mensagemLabel.setForeground(UiStyles.ERROR);
            mensagemLabel.setText("Informe o nome da conta.");
            return;
        }

        if (tipo == null) {
            mensagemLabel.setForeground(UiStyles.ERROR);
            mensagemLabel.setText("Selecione o tipo da conta.");
            return;
        }

        try {
            BigDecimal saldoInicial = moneyFormatter.parse(saldoTexto);
            saldoInicialField.setText(moneyFormatter.formatForInput(saldoInicial));
            setProcessando(true);
            aoSalvar.accept(new DadosContaForm(nome, tipo, instituicao, saldoInicial));
        } catch (ValidacaoException exception) {
            mensagemLabel.setForeground(UiStyles.ERROR);
            mensagemLabel.setText(exception.getMessage());
        }
    }

    private void preencherDados(DadosContaForm dadosIniciais) {
        if (dadosIniciais == null) {
            tipoComboBox.setSelectedItem(TipoConta.CARTEIRA);
            saldoInicialField.setText(moneyFormatter.formatForInput(BigDecimal.ZERO));
            return;
        }

        nomeField.setText(dadosIniciais.nome());
        tipoComboBox.setSelectedItem(dadosIniciais.tipo());
        instituicaoField.setText(dadosIniciais.instituicao());
        saldoInicialField.setText(moneyFormatter.formatForInput(dadosIniciais.saldoInicial()));
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
        nomeField.setEnabled(!processando);
        tipoComboBox.setEnabled(!processando);
        instituicaoField.setEnabled(!processando);
        saldoInicialField.setEnabled(!processando);
        cancelarButton.setEnabled(!processando);
        salvarButton.setEnabled(!processando);
    }
}
