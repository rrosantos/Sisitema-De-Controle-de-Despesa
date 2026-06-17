package br.com.controledespesas.view;

import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.StatusCofrinho;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public final class CofrinhoViewSupport {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final BigDecimal CEM = new BigDecimal("100");

    private CofrinhoViewSupport() {
    }

    public static String formatarStatus(StatusCofrinho status) {
        if (status == null) {
            return ViewFormatters.formatOptionalText(null);
        }

        return switch (status) {
            case EM_ANDAMENTO -> "Em andamento";
            case CONCLUIDO -> "Concluido";
            case CANCELADO -> "Cancelado";
        };
    }

    public static String formatarPercentual(BigDecimal percentual) {
        BigDecimal percentualSeguro = percentual != null ? percentual : BigDecimal.ZERO;
        NumberFormat numberFormat = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(percentualSeguro) + "%";
    }

    public static boolean estaAtrasado(Cofrinho cofrinho, LocalDate hoje) {
        if (cofrinho == null || hoje == null) {
            return false;
        }

        return cofrinho.getStatus() == StatusCofrinho.EM_ANDAMENTO
                && cofrinho.getDataLimite() != null
                && cofrinho.getDataLimite().isBefore(hoje);
    }

    public static int percentualVisual(BigDecimal percentual) {
        BigDecimal percentualSeguro = percentual != null ? percentual : BigDecimal.ZERO;
        BigDecimal percentualLimitado = percentualSeguro.max(BigDecimal.ZERO).min(CEM);
        return percentualLimitado.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static String formatarPrazo(LocalDate dataLimite) {
        return dataLimite != null ? DateFormatter.format(dataLimite) : "Sem prazo";
    }
}
