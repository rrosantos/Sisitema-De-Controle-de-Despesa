package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.MaskFormatter;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.text.ParseException;

/**
 * Centraliza mascaras e comportamento visual de inputs de data e valor.
 */
final class InputFormatters {

    private static final String MASCARA_DATA = "##/##/####";
    private static final char PLACEHOLDER_DATA = '_';

    private InputFormatters() {
    }

    static JTextField criarCampoData() {
        try {
            MaskFormatter formatter = new MaskFormatter(MASCARA_DATA);
            formatter.setPlaceholderCharacter(PLACEHOLDER_DATA);
            formatter.setAllowsInvalid(false);
            JFormattedTextField campo = new JFormattedTextField(formatter);
            campo.setFocusLostBehavior(JFormattedTextField.PERSIST);
            campo.setHorizontalAlignment(SwingConstants.CENTER);
            campo.setToolTipText("Informe a data no formato dd/MM/aaaa");
            return campo;
        } catch (ParseException exception) {
            throw new IllegalStateException("Mascara de data invalida.", exception);
        }
    }

    static String obterTextoData(JTextField campo) {
        String texto = campo.getText();
        if (texto == null) {
            return "";
        }
        String normalizado = texto.replace(String.valueOf(PLACEHOLDER_DATA), "").replace("/", "").trim();
        return normalizado.isBlank() ? "" : texto.trim();
    }

    static void instalarFormatoMonetario(JTextField campo, MoneyFormatter moneyFormatter) {
        campo.setHorizontalAlignment(SwingConstants.RIGHT);
        campo.setToolTipText("Informe o valor no formato R$0,00");
        instalarFiltroMonetario(campo);
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                campo.selectAll();
            }

            @Override
            public void focusLost(FocusEvent event) {
                formatarValor(campo, moneyFormatter);
            }
        });
    }

    static void formatarValor(JTextField campo, MoneyFormatter moneyFormatter) {
        String texto = campo.getText();
        if (texto == null || texto.trim().isBlank()) {
            return;
        }

        try {
            BigDecimal valor = moneyFormatter.parse(texto);
            campo.setText(moneyFormatter.formatForInput(valor));
        } catch (ValidacaoException exception) {
            // Mantem o texto digitado para que a validacao existente mostre a mensagem correta.
        }
    }

    static void instalarFiltroSemEspacos(JTextField campo) {
        instalarFiltro(campo, texto -> texto != null && texto.chars().noneMatch(Character::isWhitespace));
        campo.setToolTipText("Nao use espacos neste campo.");
    }

    private static void instalarFiltroMonetario(JTextField campo) {
        instalarFiltro(campo, InputFormatters::textoMonetarioValido);
    }

    private static void instalarFiltro(JTextField campo, ValidadorTexto validador) {
        if (campo.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    replace(fb, offset, 0, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text == null) {
                        return;
                    }

                    String atual = fb.getDocument().getText(0, fb.getDocument().getLength());
                    String novoTexto = atual.substring(0, offset) + text + atual.substring(offset + length);
                    if (validador.valido(novoTexto)) {
                        super.replace(fb, offset, length, text, attrs);
                    } else {
                        java.awt.Toolkit.getDefaultToolkit().beep();
                    }
                }
            });
        }
    }

    private static boolean textoMonetarioValido(String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }

        boolean possuiVirgula = false;
        int digitosDepoisDaVirgula = 0;
        for (int index = 0; index < texto.length(); index++) {
            char caractere = texto.charAt(index);
            if (Character.isDigit(caractere)) {
                if (possuiVirgula) {
                    digitosDepoisDaVirgula++;
                }
                if (digitosDepoisDaVirgula > 2) {
                    return false;
                }
                continue;
            }
            if (caractere == ',') {
                if (possuiVirgula) {
                    return false;
                }
                possuiVirgula = true;
                continue;
            }
            if (caractere == '.' || caractere == ' ' || caractere == '\u00A0'
                    || caractere == 'R' || caractere == 'r' || caractere == '$') {
                continue;
            }
            return false;
        }
        return true;
    }

    @FunctionalInterface
    private interface ValidadorTexto {

        boolean valido(String texto);
    }

}
