package br.com.controledespesas.view;

import br.com.controledespesas.dto.CofrinhoFiltro;
import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.dto.PrazoCofrinhoFiltro;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.view.component.EmptyStatePanel;
import br.com.controledespesas.view.component.LoadingPanel;
import br.com.controledespesas.view.contract.CofrinhoView;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CofrinhoPanel extends JPanel implements CofrinhoView {

    private static final String CARD_LISTA = "lista";
    private static final String CARD_VAZIO = "vazio";
    private static final String CARD_LOADING = "loading";

    private final MoneyFormatter moneyFormatter;
    private final JButton novoCofrinhoButton;
    private final JButton filtrarButton;
    private final JButton limparFiltrosButton;
    private final JTextField pesquisaField;
    private final JComboBox<SelectionOption<StatusCofrinho>> statusComboBox;
    private final JComboBox<SelectionOption<PrazoCofrinhoFiltro>> prazoComboBox;
    private final JLabel mensagemLabel;
    private final JLabel totalGuardadoLabel;
    private final JLabel metasAndamentoLabel;
    private final JLabel metasConcluidasLabel;
    private final JLabel metasCanceladasLabel;
    private final JPanel cardsContainer;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final EmptyStatePanel emptyStatePanel;

    private Runnable novoCofrinhoAction;
    private Runnable filtrarAction;
    private Runnable limparFiltrosAction;
    private Consumer<CofrinhoResumo> editarAction;
    private Consumer<CofrinhoResumo> cancelarAction;
    private Consumer<CofrinhoResumo> reativarAction;
    private Consumer<CofrinhoResumo> excluirAction;
    private Consumer<CofrinhoResumo> depositarAction;
    private Consumer<CofrinhoResumo> retirarAction;
    private Consumer<CofrinhoResumo> historicoAction;
    private boolean carregando;
    private List<CofrinhoResumo> cofrinhosExibidos = List.of();
    private CofrinhoFormDialog formularioCofrinhoAtual;
    private MovimentacaoCofrinhoDialog formularioMovimentacaoAtual;
    private HistoricoCofrinhoDialog historicoAtual;

    public CofrinhoPanel() {
        setLayout(new BorderLayout(18, 18));
        setOpaque(false);

        moneyFormatter = new MoneyFormatter();
        novoCofrinhoButton = new JButton("Novo cofrinho");
        filtrarButton = new JButton("Filtrar");
        limparFiltrosButton = new JButton("Limpar filtros");
        pesquisaField = new JTextField();
        statusComboBox = new JComboBox<>();
        prazoComboBox = new JComboBox<>();
        mensagemLabel = UiStyles.createMessageLabel();
        totalGuardadoLabel = criarResumoValor(moneyFormatter.format(BigDecimal.ZERO));
        metasAndamentoLabel = criarResumoValor("0");
        metasConcluidasLabel = criarResumoValor("0");
        metasCanceladasLabel = criarResumoValor("0");
        cardsContainer = new JPanel();
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        emptyStatePanel = new EmptyStatePanel(
                "Nenhum cofrinho cadastrado.",
                "Crie uma meta para acompanhar seu progresso financeiro.",
                "Novo cofrinho"
        );

        UiStyles.stylePrimaryButton(novoCofrinhoButton);
        UiStyles.stylePrimaryButton(filtrarButton);
        UiStyles.styleSecondaryButton(limparFiltrosButton);
        UiStyles.styleTextComponent(pesquisaField);
        UiStyles.styleComboBox(statusComboBox);
        UiStyles.styleComboBox(prazoComboBox);
        emptyStatePanel.setAcao(this::executarNovoCofrinho);

        cardsContainer.setOpaque(false);
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));

        preencherCombosFixos();
        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarConteudo(), BorderLayout.CENTER);
        configurarAcoesLocais();
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirCofrinhos(List<CofrinhoResumo> cofrinhos) {
        cofrinhosExibidos = cofrinhos != null ? List.copyOf(cofrinhos) : List.of();
        cardsContainer.removeAll();

        for (int index = 0; index < cofrinhosExibidos.size(); index++) {
            CofrinhoResumo resumo = cofrinhosExibidos.get(index);
            CofrinhoCard card = new CofrinhoCard(resumo, moneyFormatter);
            configurarAcoesCard(card, resumo);
            cardsContainer.add(card);
            if (index < cofrinhosExibidos.size() - 1) {
                cardsContainer.add(Box.createVerticalStrut(14));
            }
        }

        cardsContainer.revalidate();
        cardsContainer.repaint();
        atualizarEstadoConteudo();
    }

    @Override
    public void exibirResumoGeral(BigDecimal totalGuardado, int metasEmAndamento, int metasConcluidas,
                                  int metasCanceladas) {
        totalGuardadoLabel.setText(moneyFormatter.format(totalGuardado != null ? totalGuardado : BigDecimal.ZERO));
        metasAndamentoLabel.setText(String.valueOf(metasEmAndamento));
        metasConcluidasLabel.setText(String.valueOf(metasConcluidas));
        metasCanceladasLabel.setText(String.valueOf(metasCanceladas));
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        this.carregando = carregando;
        novoCofrinhoButton.setEnabled(!carregando);
        filtrarButton.setEnabled(!carregando);
        limparFiltrosButton.setEnabled(!carregando);
        pesquisaField.setEnabled(!carregando);
        statusComboBox.setEnabled(!carregando);
        prazoComboBox.setEnabled(!carregando);
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
    public void abrirFormularioCadastro(Consumer<DadosCofrinhoForm> aoSalvar) {
        abrirFormularioCofrinho("Novo cofrinho", null, aoSalvar);
    }

    @Override
    public void abrirFormularioEdicao(Cofrinho cofrinho, Consumer<DadosCofrinhoForm> aoSalvar) {
        abrirFormularioCofrinho("Editar cofrinho", cofrinho, aoSalvar);
    }

    @Override
    public void fecharFormularioCofrinho() {
        if (formularioCofrinhoAtual != null) {
            formularioCofrinhoAtual.fechar();
            formularioCofrinhoAtual = null;
        }
    }

    @Override
    public void exibirErroFormularioCofrinho(String mensagem) {
        if (formularioCofrinhoAtual != null) {
            formularioCofrinhoAtual.exibirErro(mensagem);
        }
    }

    @Override
    public void abrirFormularioDeposito(CofrinhoResumo resumo, Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar) {
        abrirFormularioMovimentacao("Depositar no cofrinho", resumo, aoSalvar);
    }

    @Override
    public void abrirFormularioRetirada(CofrinhoResumo resumo, Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar) {
        abrirFormularioMovimentacao("Retirar do cofrinho", resumo, aoSalvar);
    }

    @Override
    public void fecharFormularioMovimentacao() {
        if (formularioMovimentacaoAtual != null) {
            formularioMovimentacaoAtual.fechar();
            formularioMovimentacaoAtual = null;
        }
    }

    @Override
    public void exibirErroFormularioMovimentacao(String mensagem) {
        if (formularioMovimentacaoAtual != null) {
            formularioMovimentacaoAtual.exibirErro(mensagem);
        }
    }

    @Override
    public void abrirHistorico(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes,
                               Consumer<MovimentacaoCofrinho> aoExcluir) {
        if (historicoAtual == null || !historicoAtual.isDisplayable()) {
            historicoAtual = new HistoricoCofrinhoDialog(
                    SwingUtilities.getWindowAncestor(this),
                    resumo,
                    movimentacoes,
                    aoExcluir,
                    moneyFormatter
            );
            HistoricoCofrinhoDialog dialog = historicoAtual;
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if (historicoAtual == dialog) {
                        historicoAtual = null;
                    }
                }
            });
        } else {
            historicoAtual.atualizar(resumo, movimentacoes, aoExcluir);
        }

        historicoAtual.abrir();
    }

    @Override
    public void atualizarHistorico(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes) {
        if (historicoAtual != null) {
            historicoAtual.atualizar(resumo, movimentacoes);
        }
    }

    @Override
    public void fecharHistorico() {
        if (historicoAtual != null) {
            historicoAtual.fechar();
            historicoAtual = null;
        }
    }

    @Override
    public void exibirErroHistorico(String mensagem) {
        if (historicoAtual != null) {
            historicoAtual.exibirErro(mensagem);
        }
    }

    @Override
    public boolean confirmarCancelamento(Cofrinho cofrinho) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja cancelar o cofrinho \"" + cofrinho.getNome() + "\"?\n"
                        + "Novos depositos e retiradas ficarao bloqueados enquanto ele estiver cancelado.",
                "Confirmar cancelamento",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmarReativacao(Cofrinho cofrinho) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja reativar o cofrinho \"" + cofrinho.getNome() + "\"?",
                "Confirmar reativacao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmarExclusao(Cofrinho cofrinho) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja excluir o cofrinho \"" + cofrinho.getNome() + "\"?\n\n"
                        + "Todo o historico de depositos e retiradas dessa meta tambem sera excluido.\n"
                        + "Essa acao nao podera ser desfeita.",
                "Confirmar exclusao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmarExclusaoMovimentacao(MovimentacaoCofrinho movimentacao) {
        int opcao = JOptionPane.showConfirmDialog(
                this,
                "Deseja excluir esta movimentacao?\n\n"
                        + "Tipo: " + ViewFormatters.formatTipoMovimentacaoCofrinho(movimentacao.getTipo()) + "\n"
                        + "Data: " + DateFormatter.format(movimentacao.getDataMovimentacao()) + "\n"
                        + "Valor: " + moneyFormatter.format(movimentacao.getValor()),
                "Confirmar exclusao de movimentacao",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opcao == JOptionPane.YES_OPTION;
    }

    @Override
    public CofrinhoFiltro obterFiltro() {
        SelectionOption<StatusCofrinho> status = obterSelecionado(statusComboBox);
        SelectionOption<PrazoCofrinhoFiltro> prazo = obterSelecionado(prazoComboBox);
        return new CofrinhoFiltro(
                pesquisaField.getText(),
                status != null ? status.value() : null,
                prazo != null ? prazo.value() : PrazoCofrinhoFiltro.TODOS
        );
    }

    @Override
    public void limparFiltros() {
        pesquisaField.setText("");
        if (statusComboBox.getItemCount() > 0) {
            statusComboBox.setSelectedIndex(0);
        }
        if (prazoComboBox.getItemCount() > 0) {
            prazoComboBox.setSelectedIndex(0);
        }
    }

    @Override
    public void definirAcaoNovoCofrinho(Runnable acao) {
        this.novoCofrinhoAction = acao;
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
    public void definirAcaoEditar(Consumer<CofrinhoResumo> acao) {
        this.editarAction = acao;
    }

    @Override
    public void definirAcaoCancelar(Consumer<CofrinhoResumo> acao) {
        this.cancelarAction = acao;
    }

    @Override
    public void definirAcaoReativar(Consumer<CofrinhoResumo> acao) {
        this.reativarAction = acao;
    }

    @Override
    public void definirAcaoExcluir(Consumer<CofrinhoResumo> acao) {
        this.excluirAction = acao;
    }

    @Override
    public void definirAcaoDepositar(Consumer<CofrinhoResumo> acao) {
        this.depositarAction = acao;
    }

    @Override
    public void definirAcaoRetirar(Consumer<CofrinhoResumo> acao) {
        this.retirarAction = acao;
    }

    @Override
    public void definirAcaoHistorico(Consumer<CofrinhoResumo> acao) {
        this.historicoAction = acao;
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

        JLabel titulo = new JLabel("Cofrinhos");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Gerencie metas financeiras independentes de contas e transacoes.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitulo);

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.WEST);
        top.add(novoCofrinhoButton, BorderLayout.EAST);

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

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.add(criarCardResumo("Total guardado", totalGuardadoLabel));
        cards.add(criarCardResumo("Metas em andamento", metasAndamentoLabel));
        cards.add(criarCardResumo("Metas concluidas", metasConcluidasLabel));
        cards.add(criarCardResumo("Metas canceladas", metasCanceladasLabel));

        JLabel aviso = new JLabel("O resumo geral acompanha os cofrinhos visiveis apos os filtros.");
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

    private JLabel criarResumoValor(String textoInicial) {
        JLabel label = new JLabel(textoInicial);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        label.setForeground(UiStyles.TEXT_PRIMARY);
        return label;
    }

    private JPanel criarFiltros() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtros.setOpaque(false);

        pesquisaField.setPreferredSize(new Dimension(220, 40));
        statusComboBox.setPreferredSize(new Dimension(180, 40));
        prazoComboBox.setPreferredSize(new Dimension(180, 40));

        filtros.add(criarLabeled("Pesquisa por nome", pesquisaField));
        filtros.add(criarLabeled("Status", statusComboBox));
        filtros.add(criarLabeled("Prazo", prazoComboBox));
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
        JPanel listaPanel = new JPanel(new BorderLayout());
        listaPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        listaPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(listaPanel, CARD_LISTA);
        contentPanel.add(emptyStatePanel, CARD_VAZIO);
        contentPanel.add(new LoadingPanel(), CARD_LOADING);
        return contentPanel;
    }

    private void configurarAcoesLocais() {
        novoCofrinhoButton.addActionListener(event -> executarNovoCofrinho());
        filtrarButton.addActionListener(event -> executarFiltro());
        limparFiltrosButton.addActionListener(event -> executarLimpezaFiltros());
        pesquisaField.addActionListener(event -> executarFiltro());
    }

    private void preencherCombosFixos() {
        statusComboBox.removeAllItems();
        statusComboBox.addItem(new SelectionOption<>(null, "Todos"));
        statusComboBox.addItem(new SelectionOption<>(StatusCofrinho.EM_ANDAMENTO, "Em andamento"));
        statusComboBox.addItem(new SelectionOption<>(StatusCofrinho.CONCLUIDO, "Concluidos"));
        statusComboBox.addItem(new SelectionOption<>(StatusCofrinho.CANCELADO, "Cancelados"));

        prazoComboBox.removeAllItems();
        prazoComboBox.addItem(new SelectionOption<>(PrazoCofrinhoFiltro.TODOS, "Todos"));
        prazoComboBox.addItem(new SelectionOption<>(PrazoCofrinhoFiltro.COM_PRAZO, "Com prazo"));
        prazoComboBox.addItem(new SelectionOption<>(PrazoCofrinhoFiltro.SEM_PRAZO, "Sem prazo"));
        prazoComboBox.addItem(new SelectionOption<>(PrazoCofrinhoFiltro.ATRASADOS, "Atrasados"));
    }

    private void configurarAcoesCard(CofrinhoCard card, CofrinhoResumo resumo) {
        card.definirAcaoDepositar(() -> {
            if (depositarAction != null) {
                depositarAction.accept(resumo);
            }
        });
        card.definirAcaoRetirar(() -> {
            if (retirarAction != null) {
                retirarAction.accept(resumo);
            }
        });
        card.definirAcaoHistorico(() -> {
            if (historicoAction != null) {
                historicoAction.accept(resumo);
            }
        });
        card.definirAcaoEditar(() -> {
            if (editarAction != null) {
                editarAction.accept(resumo);
            }
        });
        card.definirAcaoStatus(() -> {
            if (resumo.cofrinho().getStatus() == StatusCofrinho.CANCELADO) {
                if (reativarAction != null) {
                    reativarAction.accept(resumo);
                }
            } else if (cancelarAction != null) {
                cancelarAction.accept(resumo);
            }
        });
        card.definirAcaoExcluir(() -> {
            if (excluirAction != null) {
                excluirAction.accept(resumo);
            }
        });
    }

    private void atualizarEstadoConteudo() {
        if (carregando) {
            contentLayout.show(contentPanel, CARD_LOADING);
            return;
        }

        if (cofrinhosExibidos.isEmpty()) {
            if (possuiFiltrosVisuais()) {
                emptyStatePanel.setConteudo(
                        "Nenhum cofrinho corresponde aos filtros informados.",
                        "Ajuste os filtros ou crie uma nova meta financeira."
                );
            } else {
                emptyStatePanel.setConteudo(
                        "Nenhum cofrinho cadastrado.",
                        "Crie uma meta para acompanhar seu progresso financeiro."
                );
            }
            contentLayout.show(contentPanel, CARD_VAZIO);
            return;
        }

        contentLayout.show(contentPanel, CARD_LISTA);
    }

    private boolean possuiFiltrosVisuais() {
        SelectionOption<StatusCofrinho> status = obterSelecionado(statusComboBox);
        SelectionOption<PrazoCofrinhoFiltro> prazo = obterSelecionado(prazoComboBox);
        return !pesquisaField.getText().isBlank()
                || (status != null && status.value() != null)
                || (prazo != null && prazo.value() != PrazoCofrinhoFiltro.TODOS);
    }

    private <T> SelectionOption<T> obterSelecionado(JComboBox<SelectionOption<T>> comboBox) {
        int index = comboBox.getSelectedIndex();
        return index >= 0 ? comboBox.getItemAt(index) : null;
    }

    private void executarNovoCofrinho() {
        if (novoCofrinhoAction != null) {
            novoCofrinhoAction.run();
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

    private void abrirFormularioCofrinho(String titulo, Cofrinho cofrinho, Consumer<DadosCofrinhoForm> aoSalvar) {
        fecharFormularioCofrinho();
        CofrinhoFormDialog dialog = new CofrinhoFormDialog(
                SwingUtilities.getWindowAncestor(this),
                titulo,
                cofrinho,
                aoSalvar,
                moneyFormatter
        );
        formularioCofrinhoAtual = dialog;
        dialog.abrir();
        if (formularioCofrinhoAtual == dialog && !dialog.isDisplayable()) {
            formularioCofrinhoAtual = null;
        }
    }

    private void abrirFormularioMovimentacao(String titulo, CofrinhoResumo resumo,
                                             Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar) {
        fecharFormularioMovimentacao();
        MovimentacaoCofrinhoDialog dialog = new MovimentacaoCofrinhoDialog(
                SwingUtilities.getWindowAncestor(this),
                titulo,
                Objects.requireNonNull(resumo, "resumo nao pode ser nulo."),
                aoSalvar,
                moneyFormatter
        );
        formularioMovimentacaoAtual = dialog;
        dialog.abrir();
        if (formularioMovimentacaoAtual == dialog && !dialog.isDisplayable()) {
            formularioMovimentacaoAtual = null;
        }
    }
}
