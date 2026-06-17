package br.com.controledespesas.view;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.JPasswordField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

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

    public static void applyLightComponentDefaults() {
        UIManager.put("Button.background", WHITE);
        UIManager.put("Button.foreground", PRIMARY_DARK);
        UIManager.put("Button.select", new Color(0xE8F0FF));
        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.inactiveBackground", WHITE);
        UIManager.put("TextField.inactiveForeground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background", WHITE);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", new Color(0xE8F0FF));
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
    }

    public static void stylePrimaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(WHITE);
        button.setForeground(PRIMARY_DARK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setBackground(WHITE);
        button.setForeground(PRIMARY_DARK);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
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
        if (component instanceof JPasswordField passwordField) {
            passwordField.setUI(new BasicPasswordFieldUI());
        } else if (component instanceof JTextComponent textComponent) {
            textComponent.setUI(new BasicTextFieldUI());
        }
        component.setFont(TEXT_FONT);
        component.setBorder(createInputBorder());
        component.setBackground(WHITE);
        component.setForeground(TEXT_PRIMARY);
        component.setOpaque(true);

        if (component instanceof JTextComponent textComponent) {
            textComponent.setDisabledTextColor(TEXT_PRIMARY);
            textComponent.setCaretColor(PRIMARY_DARK);
        }
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setUI(new LightComboBoxUI());
        comboBox.setFont(TEXT_FONT);
        comboBox.setBackground(WHITE);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setOpaque(true);
        comboBox.setBorder(createInputBorder());
        setLightComboBoxRenderer(comboBox);
    }

    public static JLabel createMessageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(SMALL_FONT);
        return label;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setLightComboBoxRenderer(JComboBox<?> comboBox) {
        JComboBox rawComboBox = comboBox;
        rawComboBox.setRenderer(new LightComboBoxRenderer());
    }

    private static final class LightComboBoxUI extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton("v");
            button.setUI(new BasicButtonUI());
            button.setBackground(WHITE);
            button.setForeground(TEXT_PRIMARY);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            return button;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            Color oldColor = g.getColor();
            g.setColor(WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(oldColor);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            Color oldColor = g.getColor();
            Font oldFont = g.getFont();
            Shape oldClip = g.getClip();

            g.setColor(WHITE);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            g.setFont(comboBox.getFont() != null ? comboBox.getFont() : TEXT_FONT);
            g.setColor(comboBox.isEnabled() ? TEXT_PRIMARY : TEXT_SECONDARY);

            String text = getCurrentValueText();
            FontMetrics metrics = g.getFontMetrics();
            int textX = bounds.x + 8;
            int textY = bounds.y + ((bounds.height - metrics.getHeight()) / 2) + metrics.getAscent();
            int maxWidth = Math.max(bounds.width - 16, 0);
            g.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.drawString(clipText(text, metrics, maxWidth), textX, textY);

            g.setClip(oldClip);
            g.setColor(oldColor);
            g.setFont(oldFont);
        }

        private String getCurrentValueText() {
            Object selectedItem = comboBox.getSelectedItem();
            if (selectedItem == null) {
                return "";
            }

            ListCellRenderer<Object> renderer = comboBox.getRenderer();
            Component component = renderer.getListCellRendererComponent(listBox, selectedItem, -1, false, false);
            if (component instanceof JLabel label) {
                return label.getText() != null ? label.getText() : "";
            }

            return selectedItem.toString();
        }

        private String clipText(String text, FontMetrics metrics, int maxWidth) {
            if (text == null || metrics.stringWidth(text) <= maxWidth) {
                return text != null ? text : "";
            }

            String suffix = "...";
            int suffixWidth = metrics.stringWidth(suffix);
            int availableWidth = Math.max(maxWidth - suffixWidth, 0);
            StringBuilder clipped = new StringBuilder();
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (metrics.stringWidth(clipped.toString() + character) > availableWidth) {
                    break;
                }
                clipped.append(character);
            }
            return clipped + suffix;
        }
    }

    private static final class LightComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            boolean popupItemSelected = isSelected && index >= 0;

            label.setOpaque(true);
            label.setFont(TEXT_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            label.setBackground(popupItemSelected ? new Color(0xE8F0FF) : WHITE);
            label.setForeground(popupItemSelected ? PRIMARY_DARK : TEXT_PRIMARY);

            if (list != null) {
                list.setBackground(WHITE);
                list.setForeground(TEXT_PRIMARY);
                list.setSelectionBackground(new Color(0xE8F0FF));
                list.setSelectionForeground(PRIMARY_DARK);
            }

            return label;
        }
    }
}
