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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
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
    private static final Color FUNDO_COFRINHOS = new Color(0xF5F7FB);
    private static final Color FUNDO_DESTAQUE = new Color(0xEEF4FF);
    private static final Color AZUL_DESTAQUE = new Color(0x2F6FED);
    private static final Color VERDE_DESTAQUE = new Color(0x15803D);
    private static final Color ROXO_DESTAQUE = new Color(0xA855F7);
    private static final Color LARANJA_DESTAQUE = new Color(0xEA580C);

    private final MoneyFormatter moneyFormatter;
    private final JButton novoCofrinhoButton;
    private final JButton filtrarButton;
    private final JButton limparFiltrosButton;
    private final JTextField pesquisaField;
    private final JComboBox<SelectionOption<StatusCofrinho>> statusComboBox;
    private final JComboBox<SelectionOption<PrazoCofrinhoFiltro>> prazoComboBox;
    private final JLabel mensagemLabel;
    private final DashboardSummaryCard totalGuardadoCard;
    private final DashboardSummaryCard metasAndamentoCard;
    private final DashboardSummaryCard metasConcluidasCard;
    private final DashboardSummaryCard metasCanceladasCard;
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
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(FUNDO_COFRINHOS);

        moneyFormatter = new MoneyFormatter();
        novoCofrinhoButton = new JButton("Novo cofrinho");
        filtrarButton = new JButton("Filtrar");
        limparFiltrosButton = new JButton("Limpar filtros");
        pesquisaField = new JTextField();
        statusComboBox = new JComboBox<>();
        prazoComboBox = new JComboBox<>();
        mensagemLabel = UiStyles.createMessageLabel();
        totalGuardadoCard = new DashboardSummaryCard("Total guardado");
        metasAndamentoCard = new DashboardSummaryCard("Metas em andamento");
        metasConcluidasCard = new DashboardSummaryCard("Metas concluidas");
        metasCanceladasCard = new DashboardSummaryCard("Metas canceladas");
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
        configurarCardsResumo();
        configurarCamposFiltro();
        emptyStatePanel.setAcao(this::executarNovoCofrinho);

        cardsContainer.setOpaque(false);
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));

        preencherCombosFixos();
        add(criarScrollPane(), BorderLayout.CENTER);
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
        BigDecimal total = totalGuardado != null ? totalGuardado : BigDecimal.ZERO;
        totalGuardadoCard.atualizar(
                moneyFormatter.format(total),
                cofrinhosExibidos.size() + " cofrinho(s) exibido(s)",
                "Considera o saldo atual das metas.",
                total.signum() >= 0 ? UiStyles.SUCCESS : UiStyles.ERROR
        );
        metasAndamentoCard.atualizar(
                String.valueOf(metasEmAndamento),
                "Metas recebendo movimentacoes.",
                "Dentro do resumo atual.",
                UiStyles.TEXT_PRIMARY
        );
        metasConcluidasCard.atualizar(
                String.valueOf(metasConcluidas),
                "Metas que atingiram o objetivo.",
                "Dentro do resumo atual.",
                UiStyles.SUCCESS
        );
        metasCanceladasCard.atualizar(
                String.valueOf(metasCanceladas),
                "Metas pausadas ou encerradas.",
                "Dentro do resumo atual.",
                UiStyles.TEXT_PRIMARY
        );
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
        JPanel wrapper = new JPanel(new BorderLayout(24, 0));
        wrapper.setBackground(UiStyles.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(24, 26, 24, 26)
        ));

        JPanel faixaDestaque = new JPanel();
        faixaDestaque.setBackground(AZUL_DESTAQUE);
        faixaDestaque.setPreferredSize(new Dimension(6, 0));
        wrapper.add(faixaDestaque, BorderLayout.WEST);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Cofrinhos");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Acompanhe metas financeiras, progresso e movimentacoes de cada cofrinho.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel observacaoPanel = new JPanel(new BorderLayout(10, 0));
        observacaoPanel.setBackground(FUNDO_DESTAQUE);
        observacaoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD9E6FF)),
                BorderFactory.createEmptyBorder(11, 13, 11, 13)
        ));

        JLabel indicador = new JLabel("i", SwingConstants.CENTER);
        indicador.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        indicador.setForeground(AZUL_DESTAQUE);
        indicador.setPreferredSize(new Dimension(22, 22));

        JLabel observacao = new JLabel("Cofrinhos sao metas independentes e nao reduzem automaticamente o saldo das contas.");
        observacao.setFont(UiStyles.SMALL_FONT);
        observacao.setForeground(UiStyles.TEXT_PRIMARY);

        observacaoPanel.add(indicador, BorderLayout.WEST);
        observacaoPanel.add(observacao, BorderLayout.CENTER);

        titlePanel.add(titulo);
        titlePanel.add(Box.createVerticalStrut(7));
        titlePanel.add(subtitulo);
        titlePanel.add(Box.createVerticalStrut(16));
        titlePanel.add(observacaoPanel);

        wrapper.add(titlePanel, BorderLayout.CENTER);
        wrapper.add(novoCofrinhoButton, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel criarResumo() {
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 14);
        cards.add(totalGuardadoCard, gbc);

        gbc.gridx = 1;
        cards.add(metasAndamentoCard, gbc);

        gbc.gridx = 2;
        cards.add(metasConcluidasCard, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        cards.add(metasCanceladasCard, gbc);

        return cards;
    }

    private JPanel criarFiltros() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(UiStyles.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER));

        JPanel tituloPanel = new JPanel();
        tituloPanel.setOpaque(false);
        tituloPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        tituloPanel.setLayout(new BoxLayout(tituloPanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Filtros de cofrinhos");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Busque metas por nome, status ou situacao do prazo.");
        descricao.setFont(UiStyles.SMALL_FONT);
        descricao.setForeground(UiStyles.TEXT_SECONDARY);

        tituloPanel.add(titulo);
        tituloPanel.add(Box.createVerticalStrut(4));
        tituloPanel.add(descricao);

        cabecalho.add(tituloPanel, BorderLayout.CENTER);
        panel.add(cabecalho, BorderLayout.NORTH);
        panel.add(criarFormularioFiltros(), BorderLayout.CENTER);
        panel.add(mensagemLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel criarFormularioFiltros() {
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 12);

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        filtros.add(criarLabeled("Pesquisa por nome", pesquisaField), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        filtros.add(criarLabeled("Status", statusComboBox), gbc);

        gbc.gridx = 2;
        filtros.add(criarLabeled("Prazo", prazoComboBox), gbc);

        JPanel acoes = new JPanel(new GridBagLayout());
        acoes.setBackground(new Color(0xF9FBFF));
        acoes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints acaoGbc = new GridBagConstraints();
        acaoGbc.gridy = 0;
        acaoGbc.anchor = GridBagConstraints.WEST;
        acaoGbc.insets = new Insets(0, 0, 0, 10);
        acaoGbc.gridx = 0;
        acoes.add(filtrarButton, acaoGbc);
        acaoGbc.gridx = 1;
        acaoGbc.insets = new Insets(0, 0, 0, 0);
        acoes.add(limparFiltrosButton, acaoGbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(14, 0, 0, 0);
        filtros.add(acoes, gbc);

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
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setBackground(UiStyles.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 24, 24)
        ));

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Lista de cofrinhos");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Acompanhe progresso, deposite, retire e consulte o historico de cada meta.");
        descricao.setFont(UiStyles.SMALL_FONT);
        descricao.setForeground(UiStyles.TEXT_SECONDARY);

        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(4));
        cabecalho.add(descricao);

        JPanel listaPanel = new JPanel(new BorderLayout());
        listaPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        scrollPane.setViewportBorder(null);
        scrollPane.setBackground(UiStyles.WHITE);
        scrollPane.getViewport().setBackground(UiStyles.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(0, 360));
        listaPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.setOpaque(false);
        contentPanel.add(listaPanel, CARD_LISTA);
        contentPanel.add(emptyStatePanel, CARD_VAZIO);
        contentPanel.add(new LoadingPanel(), CARD_LOADING);

        wrapper.add(cabecalho, BorderLayout.NORTH);
        wrapper.add(contentPanel, BorderLayout.CENTER);
        return wrapper;
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

    private JScrollPane criarScrollPane() {
        DashboardContentPanel conteudo = new DashboardContentPanel();
        conteudo.setBackground(FUNDO_COFRINHOS);
        conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        adicionarBloco(conteudo, criarCabecalho());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarResumo());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarFiltros());
        conteudo.add(Box.createVerticalStrut(18));
        adicionarBloco(conteudo, criarConteudo());

        JScrollPane scrollPane = new JScrollPane(conteudo);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(FUNDO_COFRINHOS);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(FUNDO_COFRINHOS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private void adicionarBloco(JPanel container, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, componente.getPreferredSize().height));
        container.add(componente);
    }

    private void configurarCardsResumo() {
        totalGuardadoCard.definirCorDestaque(AZUL_DESTAQUE);
        metasAndamentoCard.definirCorDestaque(ROXO_DESTAQUE);
        metasConcluidasCard.definirCorDestaque(VERDE_DESTAQUE);
        metasCanceladasCard.definirCorDestaque(LARANJA_DESTAQUE);
        configurarTamanhoCardResumo(totalGuardadoCard);
        configurarTamanhoCardResumo(metasAndamentoCard);
        configurarTamanhoCardResumo(metasConcluidasCard);
        configurarTamanhoCardResumo(metasCanceladasCard);
        exibirResumoGeral(BigDecimal.ZERO, 0, 0, 0);
    }

    private void configurarTamanhoCardResumo(DashboardSummaryCard card) {
        card.setPreferredSize(new Dimension(0, 154));
        card.setMinimumSize(new Dimension(180, 154));
    }

    private void configurarCamposFiltro() {
        pesquisaField.setPreferredSize(new Dimension(360, 38));
        statusComboBox.setPreferredSize(new Dimension(180, 38));
        prazoComboBox.setPreferredSize(new Dimension(180, 38));
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

    private static final class DashboardContentPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 40, 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
