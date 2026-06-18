package br.com.controledespesas.view;

import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import br.com.controledespesas.dto.ResumoContaDashboard;
import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.view.component.EmptyStatePanel;
import br.com.controledespesas.view.component.LoadingPanel;
import br.com.controledespesas.view.contract.DashboardView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.MaskFormatter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;

/**
 * Monta e atualiza a tela Swing do modulo de Inicio.
 */
public class InicioPanel extends JPanel implements DashboardView {

    private static final String ESTADO_CONTEUDO = "conteudo";
    private static final String ESTADO_VAZIO = "vazio";
    private static final String ESTADO_CARREGANDO = "carregando";

    private static final Color FUNDO_DASHBOARD = new Color(0xF5F7FB);
    private static final Color FUNDO_SUAVE = new Color(0xF8FAFC);
    private static final Color FUNDO_DESTAQUE = new Color(0xEEF4FF);
    private static final Color AZUL_DESTAQUE = new Color(0x2F6FED);
    private static final Color FUNDO_ERRO = new Color(0xFFF1F2);
    private static final Color BORDA_ERRO = new Color(0xFECACA);
    private static final Color FUNDO_CABECALHO_TABELA = new Color(0xF1F5F9);
    private static final Color FUNDO_SELECAO_TABELA = new Color(0xE8F0FF);
    private static final Color VERDE_DESTAQUE = new Color(0x15803D);
    private static final Color VERMELHO_DESTAQUE = new Color(0xB91C1C);
    private static final Color ROXO_DESTAQUE = new Color(0xA855F7);
    private static final Color LARANJA_DESTAQUE = new Color(0xEA580C);

    private static final int ESPACAMENTO_SECAO = 18;
    private static final int ALTURA_SECAO = 336;
    private static final String MASCARA_DATA = "##/##/####";

    private final JLabel boasVindasLabel;
    private final JLabel periodoAtualLabel;
    private final JLabel statusCarregamentoLabel;
    private final JLabel erroLabel;
    private final JTextField dataInicialField;
    private final JTextField dataFinalField;
    private final JButton aplicarFiltroButton;
    private final JButton atualizarButton;
    private final JButton mesAtualButton;
    private final JButton mesAnteriorButton;
    private final JButton ultimosTrintaDiasButton;
    private final JButton esteAnoButton;
    private final JButton limparPeriodoButton;
    private final JButton tentarNovamenteButton;
    private final JButton transacoesButton;
    private final JButton categoriasButton;
    private final JButton contasButton;
    private final JButton cofrinhosButton;
    private final DashboardSummaryCard saldoTotalCard;
    private final DashboardSummaryCard receitasCard;
    private final DashboardSummaryCard despesasCard;
    private final DashboardSummaryCard resultadoCard;
    private final TransacaoRecenteDashboardTableModel transacoesTableModel;
    private final JTable transacoesTable;
    private final JPanel contasBodyPanel;
    private final JPanel categoriasBodyPanel;
    private final JPanel transacoesBodyPanel;
    private final JPanel cofrinhosBodyPanel;
    private final CardLayout contasBodyLayout;
    private final CardLayout categoriasBodyLayout;
    private final CardLayout transacoesBodyLayout;
    private final CardLayout cofrinhosBodyLayout;
    private final JPanel contasListPanel;
    private final JPanel categoriasListPanel;
    private final JPanel cofrinhosListPanel;
    private final JLabel contasStatusLabel;
    private final JLabel categoriasStatusLabel;
    private final JLabel transacoesStatusLabel;
    private final JLabel cofrinhosStatusLabel;
    private final JPanel erroPanel;

    private DashboardResumo ultimoResumo;

    public InicioPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(FUNDO_DASHBOARD);

        boasVindasLabel = new JLabel("Bem-vinda(o), Usuário");
        periodoAtualLabel = new JLabel("Período: aguardando carregamento");
        statusCarregamentoLabel = new JLabel(" ");
        erroLabel = new JLabel(" ");
        dataInicialField = criarCampoData();
        dataFinalField = criarCampoData();
        aplicarFiltroButton = new JButton("Aplicar filtro");
        atualizarButton = new JButton("Atualizar");
        mesAtualButton = new JButton("Mês atual");
        mesAnteriorButton = new JButton("Mês anterior");
        ultimosTrintaDiasButton = new JButton("Últimos 30 dias");
        esteAnoButton = new JButton("Este ano");
        limparPeriodoButton = new JButton("Limpar período");
        tentarNovamenteButton = new JButton("Tentar novamente");
        transacoesButton = new JButton("Ver transações");
        categoriasButton = new JButton("Abrir categorias");
        contasButton = new JButton("Ver contas");
        cofrinhosButton = new JButton("Ver cofrinhos");
        saldoTotalCard = new DashboardSummaryCard("Saldo total das contas");
        receitasCard = new DashboardSummaryCard("Receitas recebidas");
        despesasCard = new DashboardSummaryCard("Despesas pagas");
        resultadoCard = new DashboardSummaryCard("Resultado do período");
        transacoesTableModel = new TransacaoRecenteDashboardTableModel();
        transacoesTable = new JTable(transacoesTableModel);
        contasBodyLayout = new CardLayout();
        categoriasBodyLayout = new CardLayout();
        transacoesBodyLayout = new CardLayout();
        cofrinhosBodyLayout = new CardLayout();
        contasBodyPanel = new JPanel(contasBodyLayout);
        categoriasBodyPanel = new JPanel(categoriasBodyLayout);
        transacoesBodyPanel = new JPanel(transacoesBodyLayout);
        cofrinhosBodyPanel = new JPanel(cofrinhosBodyLayout);
        contasListPanel = new JPanel();
        categoriasListPanel = new JPanel();
        cofrinhosListPanel = new JPanel();
        contasStatusLabel = new JLabel(" ");
        categoriasStatusLabel = new JLabel(" ");
        transacoesStatusLabel = new JLabel(" ");
        cofrinhosStatusLabel = new JLabel(" ");
        erroPanel = new JPanel(new BorderLayout(12, 0));

        configurarCampos();
        add(criarScrollPane(), BorderLayout.CENTER);
        exibirEstadoInicial();
    }

    public void exibirUsuario(String nome) {
        String nomeSeguro = nome != null && !nome.isBlank() ? nome : "Usuário";
        boasVindasLabel.setText("Bem-vinda(o), " + nomeSeguro);
    }

    public void definirAcaoCategorias(Runnable acao) {
        configurarAcao(categoriasButton, acao);
    }

    public void definirAcaoTransacoes(Runnable acao) {
        configurarAcao(transacoesButton, acao);
    }

    public void definirAcaoContas(Runnable acao) {
        configurarAcao(contasButton, acao);
    }

    public void definirAcaoCofrinhos(Runnable acao) {
        configurarAcao(cofrinhosButton, acao);
    }

    @Override
    public void exibirResumo(DashboardResumo resumo) {
        ultimoResumo = resumo;
        periodoAtualLabel.setText(
                "Período: " + DashboardViewSupport.formatarPeriodo(resumo.dataInicial(), resumo.dataFinal())
        );

        saldoTotalCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.saldoTotal()),
                resumo.contasAtivas() + " conta(s) ativa(s)",
                "Inclui contas ativas e inativas no saldo histórico.",
                UiStyles.TEXT_PRIMARY
        );
        receitasCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.totalReceitas()),
                "Somente receitas recebidas no período.",
                "Pendentes e canceladas não entram no total.",
                UiStyles.SUCCESS
        );
        despesasCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.totalDespesas()),
                "Somente despesas pagas no período.",
                "Pendentes e canceladas não entram no total.",
                UiStyles.ERROR
        );
        resultadoCard.atualizar(
                DashboardViewSupport.formatarResultado(resumo.resultadoPeriodo()),
                resumo.transacoesPendentes() + " transação(ões) pendente(s)",
                "Resultado = receitas recebidas - despesas pagas.",
                DashboardViewSupport.corResultado(resumo.resultadoPeriodo())
        );

        atualizarContas(resumo.contas());
        atualizarCategorias(resumo.despesasPorCategoria());
        atualizarTransacoes(resumo);
        atualizarCofrinhos(resumo.cofrinhos());
        limparIndicadoresSecao();
    }

    @Override
    public void exibirCarregamento(boolean carregando) {
        atualizarEstadoControles(carregando);
        statusCarregamentoLabel.setText(carregando ? "Atualizando resumo financeiro..." : " ");

        if (carregando && ultimoResumo == null) {
            saldoTotalCard.atualizar("—", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            receitasCard.atualizar("—", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            despesasCard.atualizar("—", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            resultadoCard.atualizar("—", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            contasBodyLayout.show(contasBodyPanel, ESTADO_CARREGANDO);
            categoriasBodyLayout.show(categoriasBodyPanel, ESTADO_CARREGANDO);
            transacoesBodyLayout.show(transacoesBodyPanel, ESTADO_CARREGANDO);
            cofrinhosBodyLayout.show(cofrinhosBodyPanel, ESTADO_CARREGANDO);
        } else if (carregando) {
            exibirIndicadoresAtualizacao();
        } else {
            if (ultimoResumo == null) {
                exibirEstadoInicial();
            }
            limparIndicadoresSecao();
        }
    }

    @Override
    public void exibirMensagemErro(String mensagem) {
        erroLabel.setText(
                mensagem != null && !mensagem.isBlank()
                        ? mensagem
                        : "Não foi possível carregar o dashboard."
        );
        erroPanel.setVisible(true);
        erroPanel.revalidate();
        erroPanel.repaint();
    }

    @Override
    public void limparErro() {
        erroLabel.setText(" ");
        erroPanel.setVisible(false);
        erroPanel.revalidate();
        erroPanel.repaint();
    }

    @Override
    public LocalDate obterDataInicial() {
        return DateFormatter.parse(obterTextoData(dataInicialField));
    }

    @Override
    public LocalDate obterDataFinal() {
        return DateFormatter.parse(obterTextoData(dataFinalField));
    }

    @Override
    public void definirPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        dataInicialField.setText(DateFormatter.format(dataInicial));
        dataFinalField.setText(DateFormatter.format(dataFinal));
        periodoAtualLabel.setText(
                "Período: " + DashboardViewSupport.formatarPeriodo(dataInicial, dataFinal)
        );
    }

    @Override
    public void definirAcaoAplicarFiltro(Runnable acao) {
        configurarAcao(aplicarFiltroButton, acao);
    }

    @Override
    public void definirAcaoAtualizar(Runnable acao) {
        configurarAcao(atualizarButton, acao);
    }

    @Override
    public void definirAcaoTentarNovamente(Runnable acao) {
        configurarAcao(tentarNovamenteButton, acao);
    }

    @Override
    public void definirAcaoMesAtual(Runnable acao) {
        configurarAcao(mesAtualButton, acao);
    }

    @Override
    public void definirAcaoMesAnterior(Runnable acao) {
        configurarAcao(mesAnteriorButton, acao);
    }

    @Override
    public void definirAcaoUltimosTrintaDias(Runnable acao) {
        configurarAcao(ultimosTrintaDiasButton, acao);
    }

    @Override
    public void definirAcaoEsteAno(Runnable acao) {
        configurarAcao(esteAnoButton, acao);
    }

    @Override
    public void definirAcaoLimparPeriodo(Runnable acao) {
        configurarAcao(limparPeriodoButton, acao);
    }

    @Override
    public void definirAcaoAbrirTransacoes(Runnable acao) {
        configurarAcao(transacoesButton, acao);
    }

    @Override
    public void definirAcaoAbrirContas(Runnable acao) {
        configurarAcao(contasButton, acao);
    }

    @Override
    public void definirAcaoAbrirCofrinhos(Runnable acao) {
        configurarAcao(cofrinhosButton, acao);
    }

    private void configurarCampos() {
        UiStyles.styleTextComponent(dataInicialField);
        UiStyles.styleTextComponent(dataFinalField);

        dataInicialField.setPreferredSize(new Dimension(128, 38));
        dataFinalField.setPreferredSize(new Dimension(128, 38));
        dataInicialField.setToolTipText("Informe a data no formato dd/MM/aaaa");
        dataFinalField.setToolTipText("Informe a data no formato dd/MM/aaaa");

        UiStyles.stylePrimaryButton(aplicarFiltroButton);
        UiStyles.styleSecondaryButton(atualizarButton);
        UiStyles.styleSecondaryButton(mesAtualButton);
        UiStyles.styleSecondaryButton(mesAnteriorButton);
        UiStyles.styleSecondaryButton(ultimosTrintaDiasButton);
        UiStyles.styleSecondaryButton(esteAnoButton);
        UiStyles.styleSecondaryButton(limparPeriodoButton);
        UiStyles.styleSecondaryButton(tentarNovamenteButton);
        UiStyles.styleLinkButton(transacoesButton);
        UiStyles.styleLinkButton(categoriasButton);
        UiStyles.styleLinkButton(contasButton);
        UiStyles.styleLinkButton(cofrinhosButton);

        saldoTotalCard.definirCorDestaque(AZUL_DESTAQUE);
        receitasCard.definirCorDestaque(VERDE_DESTAQUE);
        despesasCard.definirCorDestaque(VERMELHO_DESTAQUE);
        resultadoCard.definirCorDestaque(ROXO_DESTAQUE);

        configurarCursorBotoes(
                aplicarFiltroButton,
                atualizarButton,
                mesAtualButton,
                mesAnteriorButton,
                ultimosTrintaDiasButton,
                esteAnoButton,
                limparPeriodoButton,
                tentarNovamenteButton,
                transacoesButton,
                categoriasButton,
                contasButton,
                cofrinhosButton
        );

        periodoAtualLabel.setFont(UiStyles.LABEL_FONT);
        periodoAtualLabel.setForeground(UiStyles.TEXT_PRIMARY);

        statusCarregamentoLabel.setFont(UiStyles.SMALL_FONT);
        statusCarregamentoLabel.setForeground(AZUL_DESTAQUE);

        erroLabel.setFont(UiStyles.TEXT_FONT);
        erroLabel.setForeground(UiStyles.ERROR);

        configurarStatusLabel(contasStatusLabel);
        configurarStatusLabel(categoriasStatusLabel);
        configurarStatusLabel(transacoesStatusLabel);
        configurarStatusLabel(cofrinhosStatusLabel);

        configurarListaVertical(contasListPanel);
        configurarListaVertical(categoriasListPanel);
        configurarListaVertical(cofrinhosListPanel);

        contasBodyPanel.setOpaque(false);
        categoriasBodyPanel.setOpaque(false);
        transacoesBodyPanel.setOpaque(false);
        cofrinhosBodyPanel.setOpaque(false);

        configurarTabelaTransacoes();
    }

    private void configurarTabelaTransacoes() {
        transacoesTable.setFillsViewportHeight(true);
        transacoesTable.setRowHeight(36);
        transacoesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transacoesTable.setDefaultEditor(Object.class, null);
        transacoesTable.setShowGrid(false);
        transacoesTable.setIntercellSpacing(new Dimension(0, 0));
        transacoesTable.setSelectionBackground(FUNDO_SELECAO_TABELA);
        transacoesTable.setSelectionForeground(UiStyles.TEXT_PRIMARY);
        transacoesTable.setFont(UiStyles.TEXT_FONT);
        transacoesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        UiStyles.styleTable(transacoesTable);

        JTableHeader header = transacoesTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(UiStyles.WHITE);
        header.setForeground(UiStyles.TEXT_PRIMARY);
        header.setFont(UiStyles.LABEL_FONT);
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER)
        );
        header.setDefaultRenderer(new HeaderBrancoRenderer());
    }

    private static final class HeaderBrancoRenderer extends DefaultTableCellRenderer {

        HeaderBrancoRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setFont(UiStyles.LABEL_FONT);
            setBackground(UiStyles.WHITE);
            setForeground(UiStyles.TEXT_PRIMARY);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, UiStyles.BORDER),
                    BorderFactory.createEmptyBorder(0, 2, 0, 8)
            ));
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
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setBackground(UiStyles.WHITE);
            setForeground(UiStyles.TEXT_PRIMARY);
            setText(value != null ? value.toString() : "");
            return this;
        }
    }

    private JScrollPane criarScrollPane() {
        DashboardContentPanel conteudo = new DashboardContentPanel();
        conteudo.setBackground(FUNDO_DASHBOARD);
        conteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));

        adicionarBloco(conteudo, criarCabecalho());
        conteudo.add(Box.createVerticalStrut(ESPACAMENTO_SECAO));
        adicionarBloco(conteudo, criarPainelFiltros());
        conteudo.add(Box.createVerticalStrut(14));
        adicionarBloco(conteudo, criarPainelErro());
        conteudo.add(Box.createVerticalStrut(14));
        adicionarBloco(conteudo, criarCardsPrincipais());
        conteudo.add(Box.createVerticalStrut(ESPACAMENTO_SECAO));
        adicionarBloco(conteudo, criarGridPrincipal());

        JScrollPane scrollPane = new JScrollPane(conteudo);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(FUNDO_DASHBOARD);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(FUNDO_DASHBOARD);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel criarCabecalho() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UiStyles.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(24, 26, 24, 26)
        ));

        JPanel faixaDestaque = new JPanel();
        faixaDestaque.setBackground(AZUL_DESTAQUE);
        faixaDestaque.setPreferredSize(new Dimension(6, 0));
        panel.add(faixaDestaque, BorderLayout.WEST);

        JPanel conteudo = new JPanel(new BorderLayout(24, 0));
        conteudo.setOpaque(false);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));

        boasVindasLabel.setFont(UiStyles.TITLE_FONT);
        boasVindasLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Acompanhe sua situação financeira de forma rápida e organizada.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        textos.add(boasVindasLabel);
        textos.add(Box.createVerticalStrut(7));
        textos.add(subtitulo);

        conteudo.add(textos, BorderLayout.CENTER);
        conteudo.add(criarBadge("DASHBOARD FINANCEIRO"), BorderLayout.EAST);

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

        JLabel observacao = new JLabel(
                "<html>Os cofrinhos são independentes das contas e não reduzem automaticamente o saldo total.</html>"
        );
        observacao.setFont(UiStyles.SMALL_FONT);
        observacao.setForeground(UiStyles.TEXT_PRIMARY);

        observacaoPanel.add(indicador, BorderLayout.WEST);
        observacaoPanel.add(observacao, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 16));
        centro.setOpaque(false);
        centro.add(conteudo, BorderLayout.NORTH);
        centro.add(observacaoPanel, BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        return panel;
    }

    private JPanel criarPainelFiltros() {
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

        JLabel titulo = new JLabel("Período do dashboard");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricao = new JLabel("Escolha um intervalo ou use um dos atalhos abaixo.");
        descricao.setFont(UiStyles.SMALL_FONT);
        descricao.setForeground(UiStyles.TEXT_SECONDARY);

        tituloPanel.add(titulo);
        tituloPanel.add(Box.createVerticalStrut(4));
        tituloPanel.add(descricao);
        tituloPanel.add(Box.createVerticalStrut(8));
        tituloPanel.add(periodoAtualLabel);

        JPanel carregamentoPanel = new JPanel(new BorderLayout());
        carregamentoPanel.setOpaque(false);
        carregamentoPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 14, 0));
        carregamentoPanel.add(statusCarregamentoLabel, BorderLayout.NORTH);

        cabecalho.add(tituloPanel, BorderLayout.CENTER);
        cabecalho.add(carregamentoPanel, BorderLayout.EAST);

        panel.add(cabecalho, BorderLayout.NORTH);
        panel.add(criarFormularioFiltros(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel criarFormularioFiltros() {
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 12);

        gbc.gridx = 0;
        formulario.add(criarGrupoCampo("Data inicial", dataInicialField), gbc);

        gbc.gridx = 1;
        formulario.add(criarGrupoCampo("Data final", dataFinalField), gbc);

        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        formulario.add(aplicarFiltroButton, gbc);

        gbc.gridx = 3;
        formulario.add(atualizarButton, gbc);

        gbc.gridx = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formulario.add(Box.createHorizontalGlue(), gbc);

        JPanel atalhos = new JPanel(new GridBagLayout());
        atalhos.setBackground(new Color(0xF9FBFF));
        atalhos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints atalhoGbc = new GridBagConstraints();
        atalhoGbc.gridy = 0;
        atalhoGbc.anchor = GridBagConstraints.WEST;
        atalhoGbc.insets = new Insets(0, 0, 0, 8);

        JLabel atalhoLabel = new JLabel("Atalhos:");
        atalhoLabel.setFont(UiStyles.LABEL_FONT);
        atalhoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        atalhoGbc.gridx = 0;
        atalhos.add(atalhoLabel, atalhoGbc);
        atalhoGbc.gridx = 1;
        atalhos.add(mesAtualButton, atalhoGbc);
        atalhoGbc.gridx = 2;
        atalhos.add(mesAnteriorButton, atalhoGbc);
        atalhoGbc.gridx = 3;
        atalhos.add(ultimosTrintaDiasButton, atalhoGbc);
        atalhoGbc.gridx = 4;
        atalhos.add(esteAnoButton, atalhoGbc);
        atalhoGbc.gridx = 5;
        atalhoGbc.insets = new Insets(0, 0, 0, 0);
        atalhos.add(limparPeriodoButton, atalhoGbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 5;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(14, 0, 0, 0);
        formulario.add(atalhos, gbc);

        return formulario;
    }

    private JPanel criarGrupoCampo(String texto, JTextField campo) {
        JPanel grupo = new JPanel();
        grupo.setOpaque(false);
        grupo.setLayout(new BoxLayout(grupo, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(texto);
        label.setFont(UiStyles.LABEL_FONT);
        label.setForeground(UiStyles.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);

        grupo.add(label);
        grupo.add(Box.createVerticalStrut(6));
        grupo.add(campo);
        return grupo;
    }

    private JTextField criarCampoData() {
        try {
            MaskFormatter formatter = new MaskFormatter(MASCARA_DATA);
            formatter.setPlaceholderCharacter('_');
            formatter.setAllowsInvalid(false);

            JFormattedTextField campo = new JFormattedTextField(formatter);
            campo.setColumns(10);
            campo.setFocusLostBehavior(JFormattedTextField.PERSIST);
            return campo;
        } catch (ParseException exception) {
            throw new IllegalStateException("Mascara de data invalida.", exception);
        }
    }

    private String obterTextoData(JTextField campo) {
        String texto = campo.getText();
        if (texto == null || texto.chars().noneMatch(Character::isDigit)) {
            return "";
        }
        return texto;
    }

    private JPanel criarPainelErro() {
        erroPanel.setBackground(FUNDO_ERRO);
        erroPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA_ERRO),
                BorderFactory.createEmptyBorder(13, 15, 13, 15)
        ));
        erroPanel.setOpaque(true);

        JLabel iconeErro = new JLabel("!", SwingConstants.CENTER);
        iconeErro.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        iconeErro.setForeground(UiStyles.ERROR);
        iconeErro.setPreferredSize(new Dimension(24, 24));

        JPanel mensagemPanel = new JPanel(new BorderLayout(10, 0));
        mensagemPanel.setOpaque(false);
        mensagemPanel.add(iconeErro, BorderLayout.WEST);
        mensagemPanel.add(erroLabel, BorderLayout.CENTER);

        erroPanel.add(mensagemPanel, BorderLayout.CENTER);
        erroPanel.add(tentarNovamenteButton, BorderLayout.EAST);
        erroPanel.setVisible(false);
        return erroPanel;
    }

    private JPanel criarCardsPrincipais() {
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);

        configurarTamanhoCardResumo(saldoTotalCard);
        configurarTamanhoCardResumo(receitasCard);
        configurarTamanhoCardResumo(despesasCard);
        configurarTamanhoCardResumo(resultadoCard);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;

        constraints.gridx = 0;
        constraints.insets = new Insets(0, 0, 0, 14);
        cards.add(saldoTotalCard, constraints);

        constraints.gridx = 1;
        cards.add(receitasCard, constraints);

        constraints.gridx = 2;
        cards.add(despesasCard, constraints);

        constraints.gridx = 3;
        constraints.insets = new Insets(0, 0, 0, 0);
        cards.add(resultadoCard, constraints);

        return cards;
    }

    private JPanel criarGridPrincipal() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 16, 16);
        grid.add(criarSecaoContas(), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 16, 0);
        grid.add(criarSecaoCategorias(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 0, 16);
        grid.add(criarSecaoTransacoes(), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        grid.add(criarSecaoCofrinhos(), constraints);
        return grid;
    }

    private JPanel criarSecaoContas() {
        contasBodyPanel.add(criarScrollInterno(contasListPanel, 222), ESTADO_CONTEUDO);
        contasBodyPanel.add(new EmptyStatePanel(
                "Nenhuma conta cadastrada.",
                "Cadastre uma conta para acompanhar o saldo total e os saldos individuais."
        ), ESTADO_VAZIO);
        contasBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao(
                "Resumo das contas",
                "Os cinco maiores saldos atuais por conta.",
                contasButton,
                contasStatusLabel,
                contasBodyPanel,
                AZUL_DESTAQUE
        );
    }

    private JPanel criarSecaoCategorias() {
        categoriasBodyPanel.add(criarScrollInterno(categoriasListPanel, 222), ESTADO_CONTEUDO);
        categoriasBodyPanel.add(new EmptyStatePanel(
                "Nenhuma despesa paga no período selecionado.",
                "As categorias aparecem quando existem despesas pagas dentro do período filtrado."
        ), ESTADO_VAZIO);
        categoriasBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao(
                "Despesas por categoria",
                "Distribuição das despesas efetivamente pagas.",
                categoriasButton,
                categoriasStatusLabel,
                categoriasBodyPanel,
                ROXO_DESTAQUE
        );
    }

    private JPanel criarSecaoTransacoes() {
        transacoesBodyPanel.add(criarScrollInterno(transacoesTable, 222), ESTADO_CONTEUDO);
        transacoesBodyPanel.add(new EmptyStatePanel(
                "Nenhuma transação encontrada no período.",
                "Receitas e despesas recentes aparecem aqui em ordem decrescente de data."
        ), ESTADO_VAZIO);
        transacoesBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao(
                "Transações recentes",
                "Últimos lançamentos dentro do filtro atual.",
                transacoesButton,
                transacoesStatusLabel,
                transacoesBodyPanel,
                VERDE_DESTAQUE
        );
    }

    private JPanel criarSecaoCofrinhos() {
        JPanel conteudo = new JPanel();
        conteudo.setOpaque(false);
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.add(criarScrollInterno(cofrinhosListPanel, 196));
        conteudo.add(Box.createVerticalStrut(10));

        JPanel observacaoPanel = new JPanel(new BorderLayout());
        observacaoPanel.setBackground(FUNDO_SUAVE);
        observacaoPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel observacao = new JLabel(
                "<html>As metas são independentes das contas e não alteram o saldo total.</html>"
        );
        observacao.setFont(UiStyles.SMALL_FONT);
        observacao.setForeground(UiStyles.TEXT_SECONDARY);
        observacaoPanel.add(observacao, BorderLayout.CENTER);
        conteudo.add(observacaoPanel);

        cofrinhosBodyPanel.add(conteudo, ESTADO_CONTEUDO);
        cofrinhosBodyPanel.add(new EmptyStatePanel(
                "Nenhum cofrinho cadastrado.",
                "Crie metas para acompanhar progresso, status e prazos próximos."
        ), ESTADO_VAZIO);
        cofrinhosBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao(
                "Cofrinhos e metas",
                "Metas em andamento, priorizando os prazos mais próximos.",
                cofrinhosButton,
                cofrinhosStatusLabel,
                cofrinhosBodyPanel,
                LARANJA_DESTAQUE
        );
    }

    private JPanel criarSecao(
            String titulo,
            String descricao,
            JButton acaoButton,
            JLabel statusLabel,
            JPanel corpo,
            Color corDestaque
    ) {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(UiStyles.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));
        card.setPreferredSize(new Dimension(0, ALTURA_SECAO));
        card.setMinimumSize(new Dimension(340, ALTURA_SECAO));

        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiStyles.BORDER));

        JPanel marcador = new JPanel();
        marcador.setBackground(corDestaque != null ? corDestaque : AZUL_DESTAQUE);
        marcador.setPreferredSize(new Dimension(5, 0));

        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 0));
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        tituloLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricaoLabel = new JLabel(descricao);
        descricaoLabel.setFont(UiStyles.SMALL_FONT);
        descricaoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        statusLabel.setText(" ");

        texto.add(tituloLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(descricaoLabel);
        texto.add(Box.createVerticalStrut(5));
        texto.add(statusLabel);

        JPanel acaoPanel = new JPanel(new BorderLayout());
        acaoPanel.setOpaque(false);
        acaoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        acaoPanel.add(acaoButton, BorderLayout.NORTH);

        JPanel tituloComMarcador = new JPanel(new BorderLayout());
        tituloComMarcador.setOpaque(false);
        tituloComMarcador.add(marcador, BorderLayout.WEST);
        tituloComMarcador.add(texto, BorderLayout.CENTER);

        cabecalho.add(tituloComMarcador, BorderLayout.CENTER);
        cabecalho.add(acaoPanel, BorderLayout.EAST);

        card.add(cabecalho, BorderLayout.NORTH);
        card.add(corpo, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane criarScrollInterno(JPanel panel, int altura) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UiStyles.WHITE);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(UiStyles.WHITE);
        scrollPane.setPreferredSize(new Dimension(0, altura));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JScrollPane criarScrollInterno(JTable table, int altura) {
        JScrollPane scrollPane = new JScrollPane(table);
        UiStyles.styleTableScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(0, altura));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JLabel criarBadge(String texto) {
        JLabel badge = new JLabel(texto);
        badge.setOpaque(true);
        badge.setBackground(FUNDO_DESTAQUE);
        badge.setForeground(AZUL_DESTAQUE);
        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD9E6FF)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        return badge;
    }

    private void configurarTamanhoCardResumo(JComponent card) {
        card.setPreferredSize(new Dimension(220, 150));
        card.setMinimumSize(new Dimension(190, 145));
    }

    private void configurarStatusLabel(JLabel label) {
        label.setFont(UiStyles.SMALL_FONT);
        label.setForeground(AZUL_DESTAQUE);
    }

    private void configurarListaVertical(JPanel panel) {
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 4));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    }

    private void configurarCursorBotoes(JButton... botoes) {
        Cursor cursorMao = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        for (JButton botao : botoes) {
            botao.setCursor(cursorMao);
        }
    }

    private void adicionarBloco(JPanel container, JComponent componente) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(componente);
    }

    private void atualizarContas(List<ResumoContaDashboard> contas) {
        contasListPanel.removeAll();
        if (contas == null || contas.isEmpty()) {
            contasBodyLayout.show(contasBodyPanel, ESTADO_VAZIO);
            contasBodyPanel.revalidate();
            contasBodyPanel.repaint();
            return;
        }

        int limite = Math.min(5, contas.size());
        for (int index = 0; index < limite; index++) {
            contasListPanel.add(new ContaResumoPanel(contas.get(index)));
            if (index < limite - 1) {
                contasListPanel.add(Box.createVerticalStrut(10));
            }
        }

        contasBodyLayout.show(contasBodyPanel, ESTADO_CONTEUDO);
        contasBodyPanel.revalidate();
        contasBodyPanel.repaint();
    }

    private void atualizarCategorias(List<ResumoCategoriaDashboard> categorias) {
        categoriasListPanel.removeAll();
        if (categorias == null || categorias.isEmpty()) {
            categoriasBodyLayout.show(categoriasBodyPanel, ESTADO_VAZIO);
            categoriasBodyPanel.revalidate();
            categoriasBodyPanel.repaint();
            return;
        }

        for (int index = 0; index < categorias.size(); index++) {
            categoriasListPanel.add(new CategoriaExpenseBar(categorias.get(index)));
            if (index < categorias.size() - 1) {
                categoriasListPanel.add(Box.createVerticalStrut(10));
            }
        }

        categoriasBodyLayout.show(categoriasBodyPanel, ESTADO_CONTEUDO);
        categoriasBodyPanel.revalidate();
        categoriasBodyPanel.repaint();
    }

    private void atualizarTransacoes(DashboardResumo resumo) {
        transacoesTableModel.atualizarTransacoes(resumo.transacoesRecentes());
        if (resumo.transacoesRecentes() == null || resumo.transacoesRecentes().isEmpty()) {
            transacoesBodyLayout.show(transacoesBodyPanel, ESTADO_VAZIO);
        } else {
            transacoesBodyLayout.show(transacoesBodyPanel, ESTADO_CONTEUDO);
        }
        transacoesBodyPanel.revalidate();
        transacoesBodyPanel.repaint();
    }

    private void atualizarCofrinhos(List<ResumoCofrinhoDashboard> cofrinhos) {
        cofrinhosListPanel.removeAll();
        if (cofrinhos == null || cofrinhos.isEmpty()) {
            cofrinhosBodyLayout.show(cofrinhosBodyPanel, ESTADO_VAZIO);
            cofrinhosBodyPanel.revalidate();
            cofrinhosBodyPanel.repaint();
            return;
        }

        LocalDate hoje = LocalDate.now();
        for (int index = 0; index < cofrinhos.size(); index++) {
            cofrinhosListPanel.add(new CofrinhoResumoPanel(cofrinhos.get(index), hoje));
            if (index < cofrinhos.size() - 1) {
                cofrinhosListPanel.add(Box.createVerticalStrut(10));
            }
        }

        cofrinhosBodyLayout.show(cofrinhosBodyPanel, ESTADO_CONTEUDO);
        cofrinhosBodyPanel.revalidate();
        cofrinhosBodyPanel.repaint();
    }

    private void atualizarEstadoControles(boolean carregando) {
        aplicarFiltroButton.setEnabled(!carregando);
        atualizarButton.setEnabled(!carregando);
        mesAtualButton.setEnabled(!carregando);
        mesAnteriorButton.setEnabled(!carregando);
        ultimosTrintaDiasButton.setEnabled(!carregando);
        esteAnoButton.setEnabled(!carregando);
        limparPeriodoButton.setEnabled(!carregando);
    }

    private void exibirEstadoInicial() {
        saldoTotalCard.atualizar("—", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        receitasCard.atualizar("—", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        despesasCard.atualizar("—", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        resultadoCard.atualizar("—", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        contasBodyLayout.show(contasBodyPanel, ESTADO_VAZIO);
        categoriasBodyLayout.show(categoriasBodyPanel, ESTADO_VAZIO);
        transacoesBodyLayout.show(transacoesBodyPanel, ESTADO_VAZIO);
        cofrinhosBodyLayout.show(cofrinhosBodyPanel, ESTADO_VAZIO);
    }

    private void exibirIndicadoresAtualizacao() {
        contasStatusLabel.setText("• Atualizando dados");
        categoriasStatusLabel.setText("• Atualizando dados");
        transacoesStatusLabel.setText("• Atualizando dados");
        cofrinhosStatusLabel.setText("• Atualizando dados");
    }

    private void limparIndicadoresSecao() {
        contasStatusLabel.setText(" ");
        categoriasStatusLabel.setText(" ");
        transacoesStatusLabel.setText(" ");
        cofrinhosStatusLabel.setText(" ");
    }

    private void configurarAcao(JButton button, Runnable acao) {
        for (var listener : button.getActionListeners()) {
            button.removeActionListener(listener);
        }

        if (acao != null) {
            button.addActionListener(event -> acao.run());
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
