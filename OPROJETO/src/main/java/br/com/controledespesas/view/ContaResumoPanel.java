package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoContaDashboard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Monta e atualiza a tela Swing do modulo de ContaResumo.
 */
class ContaResumoPanel extends JPanel {

    ContaResumoPanel(ResumoContaDashboard resumo) {
        setLayout(new BorderLayout(12, 0));
        setOpaque(true);
        setBackground(new Color(0xF8FAFC));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel nomeLabel = new JLabel(ViewFormatters.formatOptionalText(resumo.nome()));
        nomeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel detalhesLabel = new JLabel(
                DashboardViewSupport.formatarTipoConta(resumo.tipo()) + " - "
                        + DashboardViewSupport.formatarStatusConta(resumo.ativa())
        );
        detalhesLabel.setFont(UiStyles.SMALL_FONT);
        detalhesLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.add(nomeLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(detalhesLabel);

        JLabel saldoLabel = new JLabel(DashboardViewSupport.formatarMoeda(resumo.saldoAtual()));
        saldoLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        saldoLabel.setForeground(resumo.saldoAtual().compareTo(java.math.BigDecimal.ZERO) < 0
                ? UiStyles.ERROR
                : UiStyles.TEXT_PRIMARY);

        add(texto, BorderLayout.CENTER);
        add(saldoLabel, BorderLayout.EAST);
    }
}
