package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Converte e formata valores monetarios no padrao brasileiro da interface.
 */
public class MoneyFormatter {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final Pattern PADRAO_MONETARIO_BR = Pattern.compile(
            "^(?:\\d+|\\d{1,3}(?:\\.\\d{3})+)(?:,\\d{1,2})?$"
    );
    private static final String PREFIXO_MOEDA = "R$";
    private static final String MENSAGEM_INVALIDA = "Informe um valor monetario valido.";
    private static final String MENSAGEM_NEGATIVA = "O saldo inicial nao pode ser negativo.";

    public BigDecimal parse(String texto) {
        if (texto == null) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        String normalizado = texto.trim()
                .replace("\u00A0", "")
                .replace(" ", "");
        if (normalizado.toUpperCase(Locale.ROOT).startsWith(PREFIXO_MOEDA)) {
            normalizado = normalizado.substring(PREFIXO_MOEDA.length());
        }
        if (normalizado.isEmpty()) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        if (normalizado.startsWith("-")) {
            throw new ValidacaoException(MENSAGEM_NEGATIVA);
        }

        if (!PADRAO_MONETARIO_BR.matcher(normalizado).matches()) {
            throw new ValidacaoException(MENSAGEM_INVALIDA);
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(normalizado.replace(".", "").replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new ValidacaoException(MENSAGEM_INVALIDA, exception);
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

        return PREFIXO_MOEDA + formatarNumero(valor);
    }

    public String formatForInput(BigDecimal valor) {
        if (valor == null) {
            return "";
        }

        return PREFIXO_MOEDA + formatarNumero(valor);
    }

    private String formatarNumero(BigDecimal valor) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        numberFormat.setGroupingUsed(true);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(valor);
    }
}
