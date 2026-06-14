package br.com.controledespesas.view.component;

import br.com.controledespesas.view.UiStyles;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class LoadingPanel extends JPanel {

    public LoadingPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel loadingLabel = new JLabel("Carregando...");
        loadingLabel.setFont(UiStyles.SUBTITLE_FONT);
        loadingLabel.setForeground(UiStyles.TEXT_SECONDARY);
        loadingLabel.setAlignmentX(CENTER_ALIGNMENT);

        content.add(Box.createVerticalGlue());
        content.add(loadingLabel);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }
}
