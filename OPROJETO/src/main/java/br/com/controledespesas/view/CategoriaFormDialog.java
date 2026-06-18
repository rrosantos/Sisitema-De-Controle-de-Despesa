package br.com.controledespesas.view;

import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.view.contract.DadosCategoriaForm;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Exibe e controla o dialogo Swing de CategoriaForm.
 */
public class CategoriaFormDialog extends JDialog {

    private final JTextField nomeField;
    private final JComboBox<TipoCategoria> tipoComboBox;
    private final JTextArea descricaoArea;
    private final JLabel mensagemLabel;
    private final JButton cancelarButton;
    private final JButton salvarButton;
    private final Consumer<DadosCategoriaForm> aoSalvar;

    private boolean processando;

    CategoriaFormDialog(Window owner, String titulo, DadosCategoriaForm dadosIniciais,
                        Consumer<DadosCategoriaForm> aoSalvar) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.aoSalvar = Objects.requireNonNull(aoSalvar, "aoSalvar nao pode ser nulo.");
        nomeField = new JTextField();
        tipoComboBox = new JComboBox<>(TipoCategoria.values());
        descricaoArea = new JTextArea(5, 20);
        mensagemLabel = UiStyles.createMessageLabel();
        cancelarButton = new JButton("Cancelar");
        salvarButton = new JButton("Salvar");
        initialize(dadosIniciais);
    }

    private void initialize(DadosCategoriaForm dadosIniciais) {
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
        setMinimumSize(new Dimension(430, 360));
        setLocationRelativeTo(getOwner());
        javax.swing.SwingUtilities.invokeLater(nomeField::requestFocusInWindow);
    }

    private JPanel criarConteudo() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        UiStyles.styleTextComponent(nomeField);
        UiStyles.styleComboBox(tipoComboBox);

        descricaoArea.setFont(UiStyles.TEXT_FONT);
        descricaoArea.setBackground(UiStyles.WHITE);
        descricaoArea.setForeground(UiStyles.TEXT_PRIMARY);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);
        descricaoArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane descricaoScrollPane = new JScrollPane(descricaoArea);
        descricaoScrollPane.setBorder(UiStyles.createInputBorder());

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
        form.add(criarCampo("Descricao", descricaoScrollPane), constraints);
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

        component.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(jLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(component);
        return panel;
    }

    private JPanel criarRodape() {
        UiStyles.styleSecondaryButton(cancelarButton);
        UiStyles.stylePrimaryButton(salvarButton);
        getRootPane().setDefaultButton(salvarButton);

        cancelarButton.addActionListener(event -> fecharSePermitido());
        salvarButton.addActionListener(event -> salvar());

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(cancelarButton);
        buttons.add(salvarButton);

        rodape.add(buttons, BorderLayout.EAST);
        return rodape;
    }

    private void salvar() {
        mensagemLabel.setText(" ");
        String nome = nomeField.getText() != null ? nomeField.getText().trim() : "";
        TipoCategoria tipo = (TipoCategoria) tipoComboBox.getSelectedItem();
        String descricao = descricaoArea.getText();

        if (nome.isBlank()) {
            mensagemLabel.setForeground(UiStyles.ERROR);
            mensagemLabel.setText("Informe o nome da categoria.");
            return;
        }

        if (tipo == null) {
            mensagemLabel.setForeground(UiStyles.ERROR);
            mensagemLabel.setText("Selecione o tipo da categoria.");
            return;
        }

        setProcessando(true);
        aoSalvar.accept(new DadosCategoriaForm(nome, tipo, descricao));
    }

    private void preencherDados(DadosCategoriaForm dadosIniciais) {
        if (dadosIniciais == null) {
            tipoComboBox.setSelectedItem(TipoCategoria.DESPESA);
            return;
        }

        nomeField.setText(dadosIniciais.nome());
        tipoComboBox.setSelectedItem(dadosIniciais.tipo());
        descricaoArea.setText(dadosIniciais.descricao());
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
        descricaoArea.setEnabled(!processando);
        cancelarButton.setEnabled(!processando);
        salvarButton.setEnabled(!processando);
    }
}
