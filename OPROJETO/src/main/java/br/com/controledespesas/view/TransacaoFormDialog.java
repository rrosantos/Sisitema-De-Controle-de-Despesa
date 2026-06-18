package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
import br.com.controledespesas.view.contract.DadosTransacaoForm;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Exibe e controla o dialogo Swing de TransacaoForm.
 */
public class TransacaoFormDialog extends JDialog {

    private static final String MENSAGEM_VALOR_ZERO = "O valor da transacao deve ser maior que zero.";

    private final JComboBox<SelectionOption<TipoTransacao>> tipoComboBox;
    private final JTextField descricaoField;
    private final JTextField valorField;
    private final JTextField dataField;
    private final JComboBox<SelectionOption<Categoria>> categoriaComboBox;
    private final JComboBox<SelectionOption<Conta>> contaComboBox;
    private final JComboBox<SelectionOption<StatusTransacao>> statusComboBox;
    private final JTextArea observacoesArea;
    private final JLabel mensagemLabel;
    private final JButton cancelarButton;
    private final JButton salvarButton;
    private final MoneyFormatter moneyFormatter;
    private final List<Categoria> categorias;
    private final List<Conta> contas;
    private final Long categoriaHistoricaId;
    private final Long contaHistoricaId;
    private final Consumer<DadosTransacaoForm> aoSalvar;

    private boolean processando;

    TransacaoFormDialog(Window owner, String titulo, Transacao transacao, List<Categoria> categorias,
                        List<Conta> contas, Consumer<DadosTransacaoForm> aoSalvar, MoneyFormatter moneyFormatter) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.categorias = List.copyOf(Objects.requireNonNull(categorias, "categorias nao podem ser nulas."));
        this.contas = List.copyOf(Objects.requireNonNull(contas, "contas nao podem ser nulas."));
        this.aoSalvar = Objects.requireNonNull(aoSalvar, "aoSalvar nao pode ser nulo.");
        this.moneyFormatter = Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");
        this.categoriaHistoricaId = transacao != null ? transacao.getCategoriaId() : null;
        this.contaHistoricaId = transacao != null ? transacao.getContaId() : null;
        tipoComboBox = new JComboBox<>();
        descricaoField = new JTextField();
        valorField = new JTextField();
        InputFormatters.instalarFormatoMonetario(valorField, this.moneyFormatter);
        dataField = InputFormatters.criarCampoData();
        categoriaComboBox = new JComboBox<>();
        contaComboBox = new JComboBox<>();
        statusComboBox = new JComboBox<>();
        observacoesArea = new JTextArea(5, 20);
        mensagemLabel = UiStyles.createMessageLabel();
        cancelarButton = new JButton("Cancelar");
        salvarButton = new JButton("Salvar");
        initialize(transacao);
    }

    private void initialize(Transacao transacao) {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(criarConteudo());
        preencherTipos();
        preencherDados(transacao);

        tipoComboBox.addActionListener(event -> atualizarDependenciasDoTipo());
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
        setMinimumSize(new Dimension(520, 520));
        setLocationRelativeTo(getOwner());
        SwingUtilities.invokeLater(descricaoField::requestFocusInWindow);
    }

    private JPanel criarConteudo() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiStyles.WHITE);
        root.setBorder(UiStyles.createCardBorder());

        UiStyles.styleTextComponent(descricaoField);
        UiStyles.styleTextComponent(valorField);
        UiStyles.styleTextComponent(dataField);
        UiStyles.styleComboBox(tipoComboBox);
        UiStyles.styleComboBox(categoriaComboBox);
        UiStyles.styleComboBox(contaComboBox);
        UiStyles.styleComboBox(statusComboBox);

        observacoesArea.setFont(UiStyles.TEXT_FONT);
        observacoesArea.setBackground(UiStyles.WHITE);
        observacoesArea.setForeground(UiStyles.TEXT_PRIMARY);
        observacoesArea.setLineWrap(true);
        observacoesArea.setWrapStyleWord(true);
        observacoesArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane observacoesScrollPane = new JScrollPane(observacoesArea);
        observacoesScrollPane.setBackground(UiStyles.WHITE);
        observacoesScrollPane.setOpaque(true);
        observacoesScrollPane.getViewport().setBackground(UiStyles.WHITE);
        observacoesScrollPane.getViewport().setOpaque(true);
        observacoesScrollPane.setViewportBorder(null);
        observacoesScrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 12, 0);

        form.add(criarCampo("Tipo", tipoComboBox), constraints);
        constraints.gridy++;
        form.add(criarCampo("Descricao", descricaoField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Valor", valorField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Data", dataField), constraints);
        constraints.gridy++;
        form.add(criarCampo("Categoria", categoriaComboBox), constraints);
        constraints.gridy++;
        form.add(criarCampo("Conta", contaComboBox), constraints);
        constraints.gridy++;
        form.add(criarCampo("Status", statusComboBox), constraints);
        constraints.gridy++;
        form.add(criarCampo("Observacoes", observacoesScrollPane), constraints);
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

    private void preencherTipos() {
        tipoComboBox.removeAllItems();
        tipoComboBox.addItem(new SelectionOption<>(TipoTransacao.RECEITA, "Receita"));
        tipoComboBox.addItem(new SelectionOption<>(TipoTransacao.DESPESA, "Despesa"));
    }

    private void preencherDados(Transacao transacao) {
        if (transacao == null) {
            selecionarTipo(TipoTransacao.RECEITA);
            valorField.setText(moneyFormatter.formatForInput(BigDecimal.ZERO));
            dataField.setText(DateFormatter.format(LocalDate.now()));
            atualizarDependenciasDoTipo();
            return;
        }

        selecionarTipo(transacao.getTipo());
        descricaoField.setText(transacao.getDescricao());
        valorField.setText(moneyFormatter.formatForInput(transacao.getValor()));
        dataField.setText(DateFormatter.format(transacao.getDataTransacao()));
        observacoesArea.setText(transacao.getObservacoes());
        atualizarStatusDisponiveis(transacao.getStatus());
        atualizarCategoriasDisponiveis(transacao.getCategoriaId());
        atualizarContasDisponiveis(transacao.getContaId());
    }

    private void selecionarTipo(TipoTransacao tipoTransacao) {
        for (int index = 0; index < tipoComboBox.getItemCount(); index++) {
            SelectionOption<TipoTransacao> option = tipoComboBox.getItemAt(index);
            if (option != null && option.value() == tipoTransacao) {
                tipoComboBox.setSelectedIndex(index);
                return;
            }
        }
    }

    private void atualizarDependenciasDoTipo() {
        SelectionOption<StatusTransacao> statusSelecionado = obterSelecionado(statusComboBox);
        SelectionOption<Categoria> categoriaSelecionada = obterSelecionado(categoriaComboBox);
        SelectionOption<Conta> contaSelecionada = obterSelecionado(contaComboBox);
        atualizarStatusDisponiveis(statusSelecionado != null ? statusSelecionado.value() : null);
        atualizarCategoriasDisponiveis(categoriaSelecionada != null && categoriaSelecionada.value() != null
                ? categoriaSelecionada.value().getId()
                : categoriaHistoricaId);
        atualizarContasDisponiveis(contaSelecionada != null && contaSelecionada.value() != null
                ? contaSelecionada.value().getId()
                : contaHistoricaId);
    }

    private void atualizarStatusDisponiveis(StatusTransacao statusSelecionado) {
        TipoTransacao tipoTransacao = obterTipoSelecionado();
        statusComboBox.removeAllItems();
        for (StatusTransacao status : TransacaoFormSupport.statusDisponiveis(tipoTransacao)) {
            statusComboBox.addItem(new SelectionOption<>(status, ViewFormatters.formatStatusTransacao(status)));
        }
        selecionarStatus(statusSelecionado);
    }

    private void atualizarCategoriasDisponiveis(Long categoriaSelecionadaId) {
        TipoTransacao tipoTransacao = obterTipoSelecionado();
        categoriaComboBox.removeAllItems();
        for (Categoria categoria : TransacaoFormSupport.categoriasDisponiveis(categorias, tipoTransacao, categoriaHistoricaId)) {
            categoriaComboBox.addItem(new SelectionOption<>(categoria, categoria.getNome()));
        }
        selecionarCategoria(categoriaSelecionadaId);
    }

    private void atualizarContasDisponiveis(Long contaSelecionadaId) {
        contaComboBox.removeAllItems();
        for (Conta conta : TransacaoFormSupport.contasDisponiveis(contas, contaHistoricaId)) {
            contaComboBox.addItem(new SelectionOption<>(conta, conta.getNome()));
        }
        selecionarConta(contaSelecionadaId);
    }

    private void selecionarStatus(StatusTransacao statusSelecionado) {
        for (int index = 0; index < statusComboBox.getItemCount(); index++) {
            SelectionOption<StatusTransacao> option = statusComboBox.getItemAt(index);
            if (option != null && option.value() == statusSelecionado) {
                statusComboBox.setSelectedIndex(index);
                return;
            }
        }
        if (statusComboBox.getItemCount() > 0) {
            statusComboBox.setSelectedIndex(0);
        }
    }

    private void selecionarCategoria(Long categoriaSelecionadaId) {
        for (int index = 0; index < categoriaComboBox.getItemCount(); index++) {
            SelectionOption<Categoria> option = categoriaComboBox.getItemAt(index);
            if (option != null && option.value() != null && Objects.equals(option.value().getId(), categoriaSelecionadaId)) {
                categoriaComboBox.setSelectedIndex(index);
                return;
            }
        }
        if (categoriaComboBox.getItemCount() > 0) {
            categoriaComboBox.setSelectedIndex(0);
        }
    }

    private void selecionarConta(Long contaSelecionadaId) {
        for (int index = 0; index < contaComboBox.getItemCount(); index++) {
            SelectionOption<Conta> option = contaComboBox.getItemAt(index);
            if (option != null && option.value() != null && Objects.equals(option.value().getId(), contaSelecionadaId)) {
                contaComboBox.setSelectedIndex(index);
                return;
            }
        }
        if (contaComboBox.getItemCount() > 0) {
            contaComboBox.setSelectedIndex(0);
        }
    }

    private void salvar() {
        mensagemLabel.setText(" ");
        TipoTransacao tipo = obterTipoSelecionado();
        String descricao = descricaoField.getText() != null ? descricaoField.getText().trim() : "";
        String valorTexto = valorField.getText();
        String dataTexto = InputFormatters.obterTextoData(dataField);
        SelectionOption<Categoria> categoriaOption = obterSelecionado(categoriaComboBox);
        SelectionOption<Conta> contaOption = obterSelecionado(contaComboBox);
        SelectionOption<StatusTransacao> statusOption = obterSelecionado(statusComboBox);
        String observacoes = observacoesArea.getText();

        if (tipo == null) {
            exibirErro("Selecione o tipo da transacao.");
            return;
        }
        if (descricao.isBlank()) {
            exibirErro("Descricao da transacao e obrigatoria.");
            return;
        }
        if (dataTexto.isBlank()) {
            exibirErro("Data da transacao e obrigatoria.");
            return;
        }
        if (categoriaOption == null || categoriaOption.value() == null || categoriaOption.value().getId() == null) {
            exibirErro("Selecione a categoria da transacao.");
            return;
        }
        if (contaOption == null || contaOption.value() == null || contaOption.value().getId() == null) {
            exibirErro("Selecione a conta da transacao.");
            return;
        }
        if (statusOption == null || statusOption.value() == null) {
            exibirErro("Selecione o status da transacao.");
            return;
        }

        try {
            BigDecimal valor = moneyFormatter.parse(valorTexto);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacaoException(MENSAGEM_VALOR_ZERO);
            }
            valorField.setText(moneyFormatter.formatForInput(valor));

            LocalDate dataTransacao = DateFormatter.parse(dataTexto);
            if (dataTransacao == null) {
                exibirErro("Data da transacao e obrigatoria.");
                return;
            }

            setProcessando(true);
            aoSalvar.accept(new DadosTransacaoForm(
                    tipo,
                    descricao,
                    valor,
                    dataTransacao,
                    categoriaOption.value().getId(),
                    contaOption.value().getId(),
                    statusOption.value(),
                    observacoes
            ));
        } catch (ValidacaoException exception) {
            exibirErro(exception.getMessage());
        }
    }

    private TipoTransacao obterTipoSelecionado() {
        SelectionOption<TipoTransacao> option = obterSelecionado(tipoComboBox);
        return option != null ? option.value() : null;
    }

    private <T> SelectionOption<T> obterSelecionado(JComboBox<SelectionOption<T>> comboBox) {
        int index = comboBox.getSelectedIndex();
        return index >= 0 ? comboBox.getItemAt(index) : null;
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
        tipoComboBox.setEnabled(!processando);
        descricaoField.setEnabled(!processando);
        valorField.setEnabled(!processando);
        dataField.setEnabled(!processando);
        categoriaComboBox.setEnabled(!processando);
        contaComboBox.setEnabled(!processando);
        statusComboBox.setEnabled(!processando);
        observacoesArea.setEnabled(!processando);
        cancelarButton.setEnabled(!processando);
        salvarButton.setEnabled(!processando);
    }
}
