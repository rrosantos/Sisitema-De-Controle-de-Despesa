package br.com.controledespesas.view;

import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;

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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Exibe e controla o dialogo Swing de CofrinhoForm.
 */
public class CofrinhoFormDialog extends JDialog {

    private final JTextField nomeField;
    private final JTextArea descricaoArea;
    private final JTextField valorMetaField;
    private final JTextField dataLimiteField;
    private final JLabel mensagemLabel;
    private final JButton cancelarButton;
    private final JButton salvarButton;
    private final MoneyFormatter moneyFormatter;
    private final Consumer<DadosCofrinhoForm> aoSalvar;

    private boolean processando;

    CofrinhoFormDialog(Window owner, String titulo, Cofrinho cofrinho, Consumer<DadosCofrinhoForm> aoSalvar,
                       MoneyFormatter moneyFormatter) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.aoSalvar = Objects.requireNonNull(aoSalvar, "aoSalvar nao pode ser nulo.");
        this.moneyFormatter = Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");
        nomeField = new JTextField();
        descricaoArea = new JTextArea(5, 20);
        valorMetaField = new JTextField();
        InputFormatters.instalarFormatoMonetario(valorMetaField, this.moneyFormatter);
        dataLimiteField = InputFormatters.criarCampoData();
        mensagemLabel = UiStyles.createMessageLabel();
        cancelarButton = new JButton("Cancelar");
        salvarButton = new JButton("Salvar");
        initialize(cofrinho);
    }

    private void initialize(Cofrinho cofrinho) {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(criarConteudo());
        preencherDados(cofrinho);

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
        setMinimumSize(new Dimension(520, 460));
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(nomeField::requestFocusInWindow);
    }

    private JPanel criarConteudo() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        UiStyles.styleTextComponent(nomeField);
        UiStyles.styleTextComponent(valorMetaField);
        UiStyles.styleTextComponent(dataLimiteField);

        descricaoArea.setFont(UiStyles.TEXT_FONT);
        descricaoArea.setBackground(UiStyles.WHITE);
        descricaoArea.setForeground(UiStyles.TEXT_PRIMARY);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);
        descricaoArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane descricaoScrollPane = new JScrollPane(descricaoArea);
        descricaoScrollPane.setBorder(UiStyles.createInputBorder());

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
        form.add(criarCampo("Descricao", descricaoScrollPane), constraints);
        constraints.gridy++;
        form.add(criarCampo("Valor da meta", valorMetaField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Data limite", dataLimiteField), constraints);
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

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(cancelarButton);
        buttons.add(salvarButton);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(buttons, BorderLayout.EAST);
        return rodape;
    }

    private void preencherDados(Cofrinho cofrinho) {
        if (cofrinho == null) {
            valorMetaField.setText(moneyFormatter.formatForInput(BigDecimal.ZERO));
            return;
        }

        nomeField.setText(cofrinho.getNome());
        descricaoArea.setText(cofrinho.getDescricao());
        valorMetaField.setText(moneyFormatter.formatForInput(cofrinho.getValorMeta()));
        dataLimiteField.setText(DateFormatter.format(cofrinho.getDataLimite()));
    }

    private void salvar() {
        mensagemLabel.setText(" ");

        try {
            DadosCofrinhoForm dados = CofrinhoFormSupport.criarDadosCofrinho(
                    nomeField.getText(),
                    descricaoArea.getText(),
                    valorMetaField.getText(),
                    InputFormatters.obterTextoData(dataLimiteField),
                    moneyFormatter
            );
            valorMetaField.setText(moneyFormatter.formatForInput(dados.valorMeta()));
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
        nomeField.setEnabled(!processando);
        descricaoArea.setEnabled(!processando);
        valorMetaField.setEnabled(!processando);
        dataLimiteField.setEnabled(!processando);
        cancelarButton.setEnabled(!processando);
        salvarButton.setEnabled(!processando);
    }
}
