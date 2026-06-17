package br.com.controledespesas.view.component;

import br.com.controledespesas.view.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class EmptyStatePanel extends JPanel {

    private final JLabel tituloLabel;
    private final JLabel descricaoLabel;
    private final JButton actionButton;

    public EmptyStatePanel(String titulo, String descricao) {
        this(titulo, descricao, null);
    }

    public EmptyStatePanel(String titulo, String descricao, String textoBotao) {
        setLayout(new BorderLayout());
        setOpaque(false);

        tituloLabel = new JLabel(titulo);
        descricaoLabel = new JLabel("<html><div style='text-align:center;'>" + descricao + "</div></html>");
        actionButton = new JButton(textoBotao != null ? textoBotao : "");

        tituloLabel.setFont(UiStyles.TITLE_FONT);
        tituloLabel.setForeground(UiStyles.TEXT_PRIMARY);
        tituloLabel.setAlignmentX(CENTER_ALIGNMENT);

        descricaoLabel.setFont(UiStyles.TEXT_FONT);
        descricaoLabel.setForeground(UiStyles.TEXT_SECONDARY);
        descricaoLabel.setAlignmentX(CENTER_ALIGNMENT);

        UiStyles.stylePrimaryButton(actionButton);
        actionButton.setVisible(textoBotao != null && !textoBotao.isBlank());
        actionButton.setAlignmentX(CENTER_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Box.createVerticalGlue());
        content.add(tituloLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(descricaoLabel);
        content.add(Box.createVerticalStrut(18));
        content.add(actionButton);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    public void setConteudo(String titulo, String descricao) {
        tituloLabel.setText(titulo);
        descricaoLabel.setText("<html><div style='text-align:center;'>" + descricao + "</div></html>");
    }

    public void setAcao(Runnable acao) {
        for (var listener : actionButton.getActionListeners()) {
            actionButton.removeActionListener(listener);
        }

        if (acao != null) {
            actionButton.addActionListener(event -> acao.run());
        }
    }
}
