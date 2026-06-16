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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

public class InicioPanel extends JPanel implements DashboardView {

    private static final String ESTADO_CONTEUDO = "conteudo";
    private static final String ESTADO_VAZIO = "vazio";
    private static final String ESTADO_CARREGANDO = "carregando";

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
        setOpaque(false);

        boasVindasLabel = new JLabel("Bem-vinda(o), Usuario.");
        periodoAtualLabel = new JLabel("Periodo: aguardando carregamento");
        statusCarregamentoLabel = new JLabel(" ");
        erroLabel = new JLabel(" ");
        dataInicialField = new JTextField(10);
        dataFinalField = new JTextField(10);
        aplicarFiltroButton = new JButton("Aplicar filtro");
        atualizarButton = new JButton("Atualizar");
        mesAtualButton = new JButton("Mes atual");
        mesAnteriorButton = new JButton("Mes anterior");
        ultimosTrintaDiasButton = new JButton("Ultimos 30 dias");
        esteAnoButton = new JButton("Este ano");
        limparPeriodoButton = new JButton("Limpar periodo");
        tentarNovamenteButton = new JButton("Tentar novamente");
        transacoesButton = new JButton("Ver todas as transacoes");
        categoriasButton = new JButton("Abrir categorias");
        contasButton = new JButton("Ver todas as contas");
        cofrinhosButton = new JButton("Ver todos os cofrinhos");
        saldoTotalCard = new DashboardSummaryCard("Saldo total das contas");
        receitasCard = new DashboardSummaryCard("Receitas recebidas");
        despesasCard = new DashboardSummaryCard("Despesas pagas");
        resultadoCard = new DashboardSummaryCard("Resultado do periodo");
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
        String nomeSeguro = nome != null && !nome.isBlank() ? nome : "Usuario";
        boasVindasLabel.setText("Bem-vinda(o), " + nomeSeguro + ".");
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
        periodoAtualLabel.setText("Periodo: " + DashboardViewSupport.formatarPeriodo(resumo.dataInicial(), resumo.dataFinal()));

        saldoTotalCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.saldoTotal()),
                resumo.contasAtivas() + " conta(s) ativa(s)",
                "Inclui contas ativas e inativas no saldo historico.",
                UiStyles.TEXT_PRIMARY
        );
        receitasCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.totalReceitas()),
                "Somente receitas recebidas no periodo.",
                "Pendentes e canceladas nao entram no total.",
                UiStyles.SUCCESS
        );
        despesasCard.atualizar(
                DashboardViewSupport.formatarMoeda(resumo.totalDespesas()),
                "Somente despesas pagas no periodo.",
                "Pendentes e canceladas nao entram no total.",
                UiStyles.ERROR
        );
        resultadoCard.atualizar(
                DashboardViewSupport.formatarResultado(resumo.resultadoPeriodo()),
                resumo.transacoesPendentes() + " transacao(oes) pendente(s)",
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
        statusCarregamentoLabel.setText(carregando ? "Carregando resumo financeiro..." : " ");

        if (carregando && ultimoResumo == null) {
            saldoTotalCard.atualizar("\u2014", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            receitasCard.atualizar("\u2014", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            despesasCard.atualizar("\u2014", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
            resultadoCard.atualizar("\u2014", "Carregando dados...", " ", UiStyles.TEXT_PRIMARY);
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
        erroLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : "Nao foi possivel carregar o dashboard.");
        erroPanel.setVisible(true);
    }

    @Override
    public void limparErro() {
        erroLabel.setText(" ");
        erroPanel.setVisible(false);
    }

    @Override
    public LocalDate obterDataInicial() {
        return DateFormatter.parse(dataInicialField.getText());
    }

    @Override
    public LocalDate obterDataFinal() {
        return DateFormatter.parse(dataFinalField.getText());
    }

    @Override
    public void definirPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        dataInicialField.setText(DateFormatter.format(dataInicial));
        dataFinalField.setText(DateFormatter.format(dataFinal));
        periodoAtualLabel.setText("Periodo: " + DashboardViewSupport.formatarPeriodo(dataInicial, dataFinal));
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

        periodoAtualLabel.setFont(UiStyles.LABEL_FONT);
        periodoAtualLabel.setForeground(UiStyles.TEXT_PRIMARY);

        statusCarregamentoLabel.setFont(UiStyles.SMALL_FONT);
        statusCarregamentoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        erroLabel.setFont(UiStyles.TEXT_FONT);
        erroLabel.setForeground(UiStyles.ERROR);

        contasStatusLabel.setFont(UiStyles.SMALL_FONT);
        contasStatusLabel.setForeground(UiStyles.TEXT_SECONDARY);
        categoriasStatusLabel.setFont(UiStyles.SMALL_FONT);
        categoriasStatusLabel.setForeground(UiStyles.TEXT_SECONDARY);
        transacoesStatusLabel.setFont(UiStyles.SMALL_FONT);
        transacoesStatusLabel.setForeground(UiStyles.TEXT_SECONDARY);
        cofrinhosStatusLabel.setFont(UiStyles.SMALL_FONT);
        cofrinhosStatusLabel.setForeground(UiStyles.TEXT_SECONDARY);

        contasListPanel.setOpaque(false);
        contasListPanel.setLayout(new BoxLayout(contasListPanel, BoxLayout.Y_AXIS));
        categoriasListPanel.setOpaque(false);
        categoriasListPanel.setLayout(new BoxLayout(categoriasListPanel, BoxLayout.Y_AXIS));
        cofrinhosListPanel.setOpaque(false);
        cofrinhosListPanel.setLayout(new BoxLayout(cofrinhosListPanel, BoxLayout.Y_AXIS));

        transacoesTable.setFillsViewportHeight(true);
        transacoesTable.setRowHeight(28);
        transacoesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transacoesTable.setDefaultEditor(Object.class, null);
        transacoesTable.getTableHeader().setReorderingAllowed(false);
    }

    private JScrollPane criarScrollPane() {
        JPanel conteudo = new JPanel();
        conteudo.setOpaque(false);
        conteudo.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.add(criarCabecalho());
        conteudo.add(Box.createVerticalStrut(20));
        conteudo.add(criarPainelFiltros());
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(criarPainelErro());
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(criarCardsPrincipais());
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(criarGridPrincipal());

        JScrollPane scrollPane = new JScrollPane(conteudo);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        return scrollPane;
    }

    private JPanel criarCabecalho() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        boasVindasLabel.setFont(UiStyles.TITLE_FONT);
        boasVindasLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Visao geral do seu controle financeiro no periodo selecionado.");
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        JTextArea observacao = new JTextArea(
                "Os cofrinhos permanecem independentes das contas. O total guardado nas metas nao e descontado automaticamente do saldo total."
        );
        observacao.setEditable(false);
        observacao.setFocusable(false);
        observacao.setOpaque(false);
        observacao.setLineWrap(true);
        observacao.setWrapStyleWord(true);
        observacao.setFont(UiStyles.TEXT_FONT);
        observacao.setForeground(UiStyles.TEXT_PRIMARY);

        panel.add(boasVindasLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitulo);
        panel.add(Box.createVerticalStrut(12));
        panel.add(observacao);
        return panel;
    }

    private JPanel criarPainelFiltros() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(UiStyles.WHITE);
        panel.setBorder(UiStyles.createCardBorder());

        JPanel tituloPanel = new JPanel();
        tituloPanel.setOpaque(false);
        tituloPanel.setLayout(new BoxLayout(tituloPanel, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Visao geral");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        tituloPanel.add(titulo);
        tituloPanel.add(Box.createVerticalStrut(6));
        tituloPanel.add(periodoAtualLabel);
        tituloPanel.add(Box.createVerticalStrut(4));
        tituloPanel.add(statusCarregamentoLabel);

        panel.add(tituloPanel, BorderLayout.WEST);
        panel.add(criarFormularioFiltros(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel criarFormularioFiltros() {
        JPanel painelDireito = new JPanel();
        painelDireito.setOpaque(false);
        painelDireito.setLayout(new BoxLayout(painelDireito, BoxLayout.Y_AXIS));

        JPanel linhaDatas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        linhaDatas.setOpaque(false);

        linhaDatas.add(criarLabelCampo("Data inicial"));
        linhaDatas.add(dataInicialField);
        linhaDatas.add(criarLabelCampo("Data final"));
        linhaDatas.add(dataFinalField);
        linhaDatas.add(aplicarFiltroButton);
        linhaDatas.add(atualizarButton);

        JPanel atalhos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        atalhos.setOpaque(false);
        atalhos.add(mesAtualButton);
        atalhos.add(mesAnteriorButton);
        atalhos.add(ultimosTrintaDiasButton);
        atalhos.add(esteAnoButton);
        atalhos.add(limparPeriodoButton);

        painelDireito.add(linhaDatas);
        painelDireito.add(Box.createVerticalStrut(12));
        painelDireito.add(atalhos);
        return painelDireito;
    }

    private JLabel criarLabelCampo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(UiStyles.LABEL_FONT);
        label.setForeground(UiStyles.TEXT_PRIMARY);
        return label;
    }

    private JPanel criarPainelErro() {
        erroPanel.setBackground(new Color(0xFFF4F4));
        erroPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        erroPanel.setOpaque(true);
        erroPanel.add(erroLabel, BorderLayout.CENTER);
        erroPanel.add(tentarNovamenteButton, BorderLayout.EAST);
        erroPanel.setVisible(false);
        return erroPanel;
    }

    private JPanel criarCardsPrincipais() {
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, 16);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;

        constraints.gridx = 0;
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
        constraints.insets = new Insets(0, 0, 16, 16);

        constraints.gridx = 0;
        constraints.gridy = 0;
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
        contasBodyPanel.setOpaque(false);
        contasBodyPanel.add(criarScrollInterno(contasListPanel, 220), ESTADO_CONTEUDO);
        contasBodyPanel.add(new EmptyStatePanel(
                "Nenhuma conta cadastrada.",
                "Cadastre uma conta para acompanhar o saldo total e os saldos individuais."
        ), ESTADO_VAZIO);
        contasBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao("Resumo das contas", "Top 5 saldos atuais por conta.", contasButton, contasStatusLabel, contasBodyPanel);
    }

    private JPanel criarSecaoCategorias() {
        categoriasBodyPanel.setOpaque(false);
        categoriasBodyPanel.add(criarScrollInterno(categoriasListPanel, 220), ESTADO_CONTEUDO);
        categoriasBodyPanel.add(new EmptyStatePanel(
                "Nenhuma despesa paga no periodo selecionado.",
                "As categorias aparecem aqui quando existem despesas pagas dentro do periodo filtrado."
        ), ESTADO_VAZIO);
        categoriasBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao("Despesas por categoria", "Somente despesas pagas entram neste resumo.", categoriasButton, categoriasStatusLabel, categoriasBodyPanel);
    }

    private JPanel criarSecaoTransacoes() {
        transacoesBodyPanel.setOpaque(false);
        transacoesBodyPanel.add(criarScrollInterno(transacoesTable, 220), ESTADO_CONTEUDO);
        transacoesBodyPanel.add(new EmptyStatePanel(
                "Nenhuma transacao encontrada no periodo.",
                "Receitas e despesas recentes aparecem aqui em ordem decrescente de data."
        ), ESTADO_VAZIO);
        transacoesBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao("Transacoes recentes", "Ultimos lancamentos dentro do filtro atual.", transacoesButton, transacoesStatusLabel, transacoesBodyPanel);
    }

    private JPanel criarSecaoCofrinhos() {
        cofrinhosBodyPanel.setOpaque(false);

        JPanel conteudo = new JPanel();
        conteudo.setOpaque(false);
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.add(criarScrollInterno(cofrinhosListPanel, 200));
        conteudo.add(Box.createVerticalStrut(10));

        JLabel observacao = new JLabel(
                "<html><body style='width:100%'>Os cofrinhos sao independentes das contas e nao reduzem o saldo total.</body></html>"
        );
        observacao.setFont(UiStyles.SMALL_FONT);
        observacao.setForeground(UiStyles.TEXT_SECONDARY);
        conteudo.add(observacao);

        cofrinhosBodyPanel.add(conteudo, ESTADO_CONTEUDO);
        cofrinhosBodyPanel.add(new EmptyStatePanel(
                "Nenhum cofrinho cadastrado.",
                "Crie metas para acompanhar progresso, status e prazos proximos."
        ), ESTADO_VAZIO);
        cofrinhosBodyPanel.add(new LoadingPanel(), ESTADO_CARREGANDO);
        return criarSecao("Cofrinhos e metas", "Metas em andamento aparecem primeiro, com foco em prazos proximos.", cofrinhosButton, cofrinhosStatusLabel, cofrinhosBodyPanel);
    }

    private JPanel criarSecao(String titulo, String descricao, JButton acaoButton,
                              JLabel statusLabel, JPanel corpo) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(UiStyles.WHITE);
        card.setBorder(UiStyles.createCardBorder());
        card.setPreferredSize(new Dimension(0, 320));

        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setOpaque(false);

        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tituloLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel descricaoLabel = new JLabel(descricao);
        descricaoLabel.setFont(UiStyles.SMALL_FONT);
        descricaoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        statusLabel.setText(" ");

        texto.add(tituloLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(descricaoLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(statusLabel);

        cabecalho.add(texto, BorderLayout.CENTER);
        cabecalho.add(acaoButton, BorderLayout.EAST);

        card.add(cabecalho, BorderLayout.NORTH);
        card.add(corpo, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane criarScrollInterno(JPanel panel, int altura) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(0, altura));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JScrollPane criarScrollInterno(JTable table, int altura) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, altura));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
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
        saldoTotalCard.atualizar("\u2014", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        receitasCard.atualizar("\u2014", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        despesasCard.atualizar("\u2014", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        resultadoCard.atualizar("\u2014", "Aguardando carregamento.", " ", UiStyles.TEXT_PRIMARY);
        contasBodyLayout.show(contasBodyPanel, ESTADO_VAZIO);
        categoriasBodyLayout.show(categoriasBodyPanel, ESTADO_VAZIO);
        transacoesBodyLayout.show(transacoesBodyPanel, ESTADO_VAZIO);
        cofrinhosBodyLayout.show(cofrinhosBodyPanel, ESTADO_VAZIO);
    }

    private void exibirIndicadoresAtualizacao() {
        contasStatusLabel.setText("Atualizando...");
        categoriasStatusLabel.setText("Atualizando...");
        transacoesStatusLabel.setText("Atualizando...");
        cofrinhosStatusLabel.setText("Atualizando...");
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
}
