package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

final class UiStyles {

    static final Color BACKGROUND = new Color(0xF4F7FB);
    static final Color PRIMARY = new Color(0x2563EB);
    static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    static final Color TEXT_PRIMARY = new Color(0x1F2937);
    static final Color TEXT_SECONDARY = new Color(0x6B7280);
    static final Color BORDER = new Color(0xDCE3ED);
    static final Color WHITE = Color.WHITE;
    static final Color ERROR = new Color(0xB91C1C);
    static final Color SUCCESS = new Color(0x15803D);

    static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    static final Font SUBTITLE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private UiStyles() {
    }

    static Border createInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)
        );
    }

    static void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void styleSecondaryButton(JButton button) {
        button.setBackground(WHITE);
        button.setForeground(PRIMARY_DARK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void styleLinkButton(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(PRIMARY_DARK);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void styleTextComponent(JComponent component) {
        component.setFont(TEXT_FONT);
        component.setBorder(createInputBorder());
        component.setBackground(WHITE);
        component.setForeground(TEXT_PRIMARY);
    }

    static JLabel createMessageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(SMALL_FONT);
        return label;
    }
}
