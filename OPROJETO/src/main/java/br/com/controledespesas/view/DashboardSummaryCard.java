package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

class DashboardSummaryCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel primaryDetailLabel;
    private final JLabel secondaryDetailLabel;

    DashboardSummaryCard(String titulo) {
        setLayout(new BorderLayout());
        setBackground(UiStyles.WHITE);
        setBorder(UiStyles.createCardBorder());
        setPreferredSize(new Dimension(0, 160));

        titleLabel = new JLabel(titulo);
        valueLabel = new JLabel("\u2014");
        primaryDetailLabel = new JLabel(" ");
        secondaryDetailLabel = new JLabel(" ");

        titleLabel.setFont(UiStyles.LABEL_FONT);
        titleLabel.setForeground(UiStyles.TEXT_SECONDARY);

        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        valueLabel.setForeground(UiStyles.TEXT_PRIMARY);

        primaryDetailLabel.setFont(UiStyles.TEXT_FONT);
        primaryDetailLabel.setForeground(UiStyles.TEXT_PRIMARY);

        secondaryDetailLabel.setFont(UiStyles.SMALL_FONT);
        secondaryDetailLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(10));
        content.add(primaryDetailLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(secondaryDetailLabel);

        add(content, BorderLayout.CENTER);
    }

    void atualizar(String valor, String detalhePrimario, String detalheSecundario, Color corValor) {
        valueLabel.setText(valor != null && !valor.isBlank() ? valor : "\u2014");
        primaryDetailLabel.setText(detalhePrimario != null && !detalhePrimario.isBlank() ? detalhePrimario : " ");
        secondaryDetailLabel.setText(detalheSecundario != null && !detalheSecundario.isBlank() ? detalheSecundario : " ");
        valueLabel.setForeground(corValor != null ? corValor : UiStyles.TEXT_PRIMARY);
    }
}
