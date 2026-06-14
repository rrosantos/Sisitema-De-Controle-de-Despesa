package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

public final class UiStyles {

    public static final Color BACKGROUND = new Color(0xF4F7FB);
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    public static final Color TEXT_PRIMARY = new Color(0x1F2937);
    public static final Color TEXT_SECONDARY = new Color(0x6B7280);
    public static final Color BORDER = new Color(0xDCE3ED);
    public static final Color WHITE = Color.WHITE;
    public static final Color ERROR = new Color(0xB91C1C);
    public static final Color SUCCESS = new Color(0x15803D);

    public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    public static final Font SUBTITLE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private UiStyles() {
    }

    public static Border createInputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        );
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(28, 28, 28, 28)
        );
    }

    public static void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton button) {
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

    public static void styleLinkButton(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(PRIMARY_DARK);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleTextComponent(JComponent component) {
        component.setFont(TEXT_FONT);
        component.setBorder(createInputBorder());
        component.setBackground(WHITE);
        component.setForeground(TEXT_PRIMARY);
    }

    public static JLabel createMessageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(SMALL_FONT);
        return label;
    }
}
