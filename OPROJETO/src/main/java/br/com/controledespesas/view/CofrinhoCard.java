package br.com.controledespesas.view;

import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.model.StatusCofrinho;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;

/**
 * Componente visual que resume dados de Cofrinho em formato de card.
 */
class CofrinhoCard extends JPanel {

    private final JButton depositarButton;
    private final JButton retirarButton;
    private final JButton historicoButton;
    private final JButton editarButton;
    private final JButton statusButton;
    private final JButton excluirButton;

    CofrinhoCard(CofrinhoResumo resumo, MoneyFormatter moneyFormatter) {
        setLayout(new BorderLayout(0, 18));
        setBackground(UiStyles.WHITE);
        setBorder(UiStyles.createCardBorder());
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        depositarButton = new JButton("Depositar");
        retirarButton = new JButton("Retirar");
        historicoButton = new JButton("Historico");
        editarButton = new JButton("Editar");
        statusButton = new JButton(resumo.cofrinho().getStatus() == StatusCofrinho.CANCELADO
                ? "Reativar meta"
                : "Cancelar meta");
        excluirButton = new JButton("Excluir");

        UiStyles.styleSecondaryButton(depositarButton);
        UiStyles.styleSecondaryButton(retirarButton);
        UiStyles.styleSecondaryButton(historicoButton);
        UiStyles.styleSecondaryButton(editarButton);
        UiStyles.styleSecondaryButton(statusButton);
        UiStyles.styleSecondaryButton(excluirButton);

        add(criarConteudo(resumo, moneyFormatter), BorderLayout.CENTER);
        add(criarAcoes(resumo), BorderLayout.SOUTH);
    }

    void definirAcaoDepositar(Runnable acao) {
        configurarAcao(depositarButton, acao);
    }

    void definirAcaoRetirar(Runnable acao) {
        configurarAcao(retirarButton, acao);
    }

    void definirAcaoHistorico(Runnable acao) {
        configurarAcao(historicoButton, acao);
    }

    void definirAcaoEditar(Runnable acao) {
        configurarAcao(editarButton, acao);
    }

    void definirAcaoStatus(Runnable acao) {
        configurarAcao(statusButton, acao);
    }

    void definirAcaoExcluir(Runnable acao) {
        configurarAcao(excluirButton, acao);
    }

    private JPanel criarConteudo(CofrinhoResumo resumo, MoneyFormatter moneyFormatter) {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));

        JLabel nomeLabel = new JLabel(resumo.cofrinho().getNome());
        nomeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel statusLabel = new JLabel("Status: " + CofrinhoViewSupport.formatarStatus(resumo.cofrinho().getStatus()));
        statusLabel.setFont(UiStyles.LABEL_FONT);
        statusLabel.setForeground(UiStyles.TEXT_SECONDARY);

        cabecalho.add(nomeLabel);
        cabecalho.add(Box.createVerticalStrut(6));
        cabecalho.add(statusLabel);

        if (CofrinhoViewSupport.estaAtrasado(resumo.cofrinho(), LocalDate.now())) {
            JLabel atrasadoLabel = new JLabel("Situacao: Atrasada");
            atrasadoLabel.setFont(UiStyles.SMALL_FONT);
            atrasadoLabel.setForeground(UiStyles.ERROR);
            cabecalho.add(Box.createVerticalStrut(4));
            cabecalho.add(atrasadoLabel);
        }

        JTextArea descricaoArea = new JTextArea(
                resumo.cofrinho().getDescricao() != null && !resumo.cofrinho().getDescricao().isBlank()
                        ? resumo.cofrinho().getDescricao()
                        : "Sem descricao"
        );
        descricaoArea.setEditable(false);
        descricaoArea.setFocusable(false);
        descricaoArea.setOpaque(false);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);
        descricaoArea.setFont(UiStyles.TEXT_FONT);
        descricaoArea.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel resumoFinanceiro = new JPanel();
        resumoFinanceiro.setOpaque(false);
        resumoFinanceiro.setLayout(new BoxLayout(resumoFinanceiro, BoxLayout.Y_AXIS));

        JLabel valoresLabel = new JLabel(
                moneyFormatter.format(resumo.valorAtual()) + " de " + moneyFormatter.format(resumo.cofrinho().getValorMeta())
        );
        valoresLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        valoresLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel percentualLabel = new JLabel(CofrinhoViewSupport.formatarPercentual(resumo.percentualProgresso()));
        percentualLabel.setFont(UiStyles.TEXT_FONT);
        percentualLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(CofrinhoViewSupport.percentualVisual(resumo.percentualProgresso()));
        progressBar.setStringPainted(false);
        progressBar.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        progressBar.setBackground(UiStyles.BACKGROUND);
        progressBar.setForeground(UiStyles.SUCCESS);
        progressBar.setPreferredSize(new Dimension(0, 18));

        JLabel prazoLabel = new JLabel("Prazo: " + CofrinhoViewSupport.formatarPrazo(resumo.cofrinho().getDataLimite()));
        prazoLabel.setFont(UiStyles.TEXT_FONT);
        prazoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        resumoFinanceiro.add(descricaoArea);
        resumoFinanceiro.add(Box.createVerticalStrut(12));
        resumoFinanceiro.add(valoresLabel);
        resumoFinanceiro.add(Box.createVerticalStrut(4));
        resumoFinanceiro.add(percentualLabel);
        resumoFinanceiro.add(Box.createVerticalStrut(8));
        resumoFinanceiro.add(progressBar);
        resumoFinanceiro.add(Box.createVerticalStrut(10));
        resumoFinanceiro.add(prazoLabel);

        content.add(cabecalho, BorderLayout.NORTH);
        content.add(resumoFinanceiro, BorderLayout.CENTER);
        return content;
    }

    private JPanel criarAcoes(CofrinhoResumo resumo) {
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        acoes.setOpaque(false);

        depositarButton.setToolTipText("Registrar deposito nesta meta");
        retirarButton.setToolTipText("Registrar retirada nesta meta");
        historicoButton.setToolTipText("Consultar historico de movimentacoes");
        editarButton.setToolTipText("Editar nome, descricao, meta e prazo");
        excluirButton.setToolTipText("Excluir cofrinho e todo o historico");

        if (resumo.cofrinho().getStatus() == StatusCofrinho.CANCELADO) {
            depositarButton.setEnabled(false);
            retirarButton.setEnabled(false);
        }

        acoes.add(depositarButton);
        acoes.add(retirarButton);
        acoes.add(historicoButton);
        acoes.add(editarButton);
        acoes.add(statusButton);
        acoes.add(excluirButton);
        return acoes;
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
