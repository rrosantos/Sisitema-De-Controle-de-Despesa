package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Sistema de Controle de Despesas Pessoais");
        initialize();
    }

    private void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setMinimumSize(new Dimension(720, 420));
        setLocationRelativeTo(null);
        setContentPane(createContentPanel());
    }

    private JPanel createContentPanel() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        rootPanel.setBackground(new Color(245, 247, 250));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Sistema de Controle de Despesas Pessoais", SwingConstants.CENTER);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));

        JLabel statusLabel = new JLabel("Projeto configurado e conectado ao MySQL.", SwingConstants.CENTER);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        JTextArea detailsArea = new JTextArea(
                "A estrutura inicial do projeto foi preparada com Java, Swing, Maven e JDBC.\n" +
                "As funcionalidades de cadastro, login, transacoes, contas, categorias e cofrinhos " +
                "serao adicionadas nas proximas etapas."
        );
        detailsArea.setEditable(false);
        detailsArea.setFocusable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setOpaque(false);
        detailsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(18));
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalStrut(24));
        contentPanel.add(detailsArea);

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        return rootPanel;
    }
}
