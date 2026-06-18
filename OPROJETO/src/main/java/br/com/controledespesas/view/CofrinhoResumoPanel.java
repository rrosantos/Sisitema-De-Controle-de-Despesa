package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoCofrinhoDashboard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;

/**
 * Monta e atualiza a tela Swing do modulo de CofrinhoResumo.
 */
class CofrinhoResumoPanel extends JPanel {

    CofrinhoResumoPanel(ResumoCofrinhoDashboard resumo, LocalDate hoje) {
        setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(new Color(0xF8FAFC));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel nomeLabel = new JLabel(ViewFormatters.formatOptionalText(resumo.nome()));
        nomeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        String statusTexto = DashboardViewSupport.formatarStatusCofrinho(resumo.status());
        if (DashboardViewSupport.metaAtrasada(resumo, hoje)) {
            statusTexto += " - Atrasado";
        }

        JLabel statusLabel = new JLabel(statusTexto);
        statusLabel.setFont(UiStyles.SMALL_FONT);
        statusLabel.setForeground(DashboardViewSupport.metaAtrasada(resumo, hoje)
                ? UiStyles.ERROR
                : UiStyles.TEXT_SECONDARY);

        JLabel valoresLabel = new JLabel(
                DashboardViewSupport.formatarMoeda(resumo.valorAtual())
                        + " de "
                        + DashboardViewSupport.formatarMoeda(resumo.valorMeta())
        );
        valoresLabel.setFont(UiStyles.TEXT_FONT);
        valoresLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel percentualLabel = new JLabel(CofrinhoViewSupport.formatarPercentual(resumo.percentual()));
        percentualLabel.setFont(UiStyles.SMALL_FONT);
        percentualLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JLabel prazoLabel = new JLabel("Prazo: " + DashboardViewSupport.formatarPrazo(resumo.dataLimite()));
        prazoLabel.setFont(UiStyles.SMALL_FONT);
        prazoLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(DashboardViewSupport.percentualVisual(resumo.percentual()));
        progressBar.setPreferredSize(new Dimension(0, 12));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setBackground(new Color(0xE5E7EB));
        progressBar.setForeground(UiStyles.SUCCESS);
        progressBar.setStringPainted(false);

        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.add(nomeLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(statusLabel);
        texto.add(Box.createVerticalStrut(6));
        texto.add(valoresLabel);
        texto.add(Box.createVerticalStrut(4));
        texto.add(percentualLabel);
        texto.add(Box.createVerticalStrut(8));
        texto.add(progressBar);
        texto.add(Box.createVerticalStrut(6));
        texto.add(prazoLabel);

        add(texto, BorderLayout.CENTER);
    }
}
