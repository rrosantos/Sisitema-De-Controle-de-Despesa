package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

public class MoneyFormatter {

    private static final Locale LOCALE_PT_BR = new Locale("pt", "BR");
    private static final String MENSAGEM_INVALIDA = "Informe um valor monetario valido.";
    private static final String MENSAGEM_NEGATIVA = "O saldo inicial nao pode ser negativo.";

    public BigDecimal parse(String texto) {
        if (texto == null) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        String normalizado = texto.trim();
        if (normalizado.isEmpty()) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_PT_BR);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.##", symbols);
        decimalFormat.setParseBigDecimal(true);
        decimalFormat.setGroupingUsed(true);

        ParsePosition parsePosition = new ParsePosition(0);
        Object parsed = decimalFormat.parse(normalizado, parsePosition);

        if (!(parsed instanceof BigDecimal valor) || parsePosition.getIndex() != normalizado.length()) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        BigDecimal valorNormalizado = valor.setScale(2, RoundingMode.HALF_UP);
        if (valorNormalizado.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacaoException(MENSAGEM_NEGATIVA);
        }

        return valorNormalizado;
    }

    public String format(BigDecimal valor) {
        if (valor == null) {
            return ViewFormatters.formatOptionalText(null);
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
        return currencyFormat.format(valor);
    }

    public String formatForInput(BigDecimal valor) {
        if (valor == null) {
            return "";
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_PT_BR);
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", symbols);
        decimalFormat.setParseBigDecimal(true);
        decimalFormat.setGroupingUsed(true);
        return decimalFormat.format(valor);
    }
}
