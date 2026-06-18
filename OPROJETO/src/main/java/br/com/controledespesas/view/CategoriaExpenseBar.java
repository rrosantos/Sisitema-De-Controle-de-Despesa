package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoCategoriaDashboard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Define responsabilidades de CategoriaExpenseBar dentro do sistema.
 */
class CategoriaExpenseBar extends JPanel {

    private final JLabel nomeLabel;
    private final JLabel valorLabel;
    private final JLabel percentualLabel;
    private final JProgressBar progressBar;

    CategoriaExpenseBar(ResumoCategoriaDashboard resumo) {
        setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(new java.awt.Color(0xF8FAFC));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        nomeLabel = new JLabel();
        valorLabel = new JLabel();
        percentualLabel = new JLabel();
        progressBar = new JProgressBar(0, 100);

        nomeLabel.setFont(UiStyles.LABEL_FONT);
        nomeLabel.setForeground(UiStyles.TEXT_PRIMARY);

        valorLabel.setFont(UiStyles.TEXT_FONT);
        valorLabel.setForeground(UiStyles.TEXT_PRIMARY);

        percentualLabel.setFont(UiStyles.SMALL_FONT);
        percentualLabel.setForeground(UiStyles.TEXT_SECONDARY);

        progressBar.setPreferredSize(new Dimension(0, 12));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setBackground(new java.awt.Color(0xE5E7EB));
        progressBar.setForeground(UiStyles.PRIMARY);
        progressBar.setStringPainted(false);

        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setOpaque(false);
        cabecalho.add(nomeLabel, BorderLayout.WEST);

        JPanel valores = new JPanel();
        valores.setOpaque(false);
        valores.setLayout(new BoxLayout(valores, BoxLayout.X_AXIS));
        valores.add(valorLabel);
        valores.add(Box.createHorizontalStrut(12));
        valores.add(percentualLabel);
        cabecalho.add(valores, BorderLayout.EAST);

        add(cabecalho, BorderLayout.NORTH);
        add(progressBar, BorderLayout.CENTER);

        atualizar(resumo);
    }

    void atualizar(ResumoCategoriaDashboard resumo) {
        nomeLabel.setText(ViewFormatters.formatOptionalText(resumo != null ? resumo.nomeCategoria() : null));
        valorLabel.setText(DashboardViewSupport.formatarMoeda(resumo != null ? resumo.total() : null));
        percentualLabel.setText(CofrinhoViewSupport.formatarPercentual(resumo != null ? resumo.percentual() : null));
        progressBar.setValue(calcularPercentualVisual(resumo != null ? resumo.percentual() : null));
    }

    static int calcularPercentualVisual(java.math.BigDecimal percentual) {
        return DashboardViewSupport.percentualVisual(percentual);
    }

    String obterTextoValor() {
        return valorLabel.getText();
    }

    String obterTextoPercentual() {
        return percentualLabel.getText();
    }
}
