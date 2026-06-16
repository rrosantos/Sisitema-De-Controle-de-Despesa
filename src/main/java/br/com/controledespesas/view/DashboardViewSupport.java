package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoConta;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

final class DashboardViewSupport {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final MoneyFormatter MONEY_FORMATTER = new MoneyFormatter();

    private DashboardViewSupport() {
    }

    static String formatarMoeda(BigDecimal valor) {
        BigDecimal valorSeguro = valor != null ? valor : BigDecimal.ZERO.setScale(2);
        return MONEY_FORMATTER.format(valorSeguro);
    }

    static String formatarResultado(BigDecimal valor) {
        BigDecimal valorSeguro = valor != null ? valor : BigDecimal.ZERO.setScale(2);
        if (valorSeguro.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + MONEY_FORMATTER.format(valorSeguro);
        }
        return MONEY_FORMATTER.format(valorSeguro);
    }

    static Color corResultado(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            return UiStyles.TEXT_PRIMARY;
        }
        return valor.compareTo(BigDecimal.ZERO) > 0 ? UiStyles.SUCCESS : UiStyles.ERROR;
    }

    static int percentualVisual(BigDecimal percentual) {
        BigDecimal percentualSeguro = percentual != null ? percentual : BigDecimal.ZERO;
        BigDecimal percentualLimitado = percentualSeguro.max(BigDecimal.ZERO).min(CEM);
        return percentualLimitado.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    static boolean metaAtrasada(ResumoCofrinhoDashboard resumo, LocalDate hoje) {
        if (resumo == null || hoje == null || resumo.dataLimite() == null) {
            return false;
        }

        return resumo.status() == StatusCofrinho.EM_ANDAMENTO && resumo.dataLimite().isBefore(hoje);
    }

    static String formatarStatusConta(boolean ativa) {
        return ViewFormatters.formatStatus(ativa);
    }

    static String formatarStatusCofrinho(StatusCofrinho status) {
        return CofrinhoViewSupport.formatarStatus(status);
    }

    static String formatarTipoConta(TipoConta tipoConta) {
        return ViewFormatters.formatTipoConta(tipoConta);
    }

    static String formatarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null && dataFinal == null) {
            return "Todos os registros";
        }
        if (dataInicial == null) {
            return "Ate " + DateFormatter.format(dataFinal);
        }
        if (dataFinal == null) {
            return "Desde " + DateFormatter.format(dataInicial);
        }
        return DateFormatter.format(dataInicial) + " a " + DateFormatter.format(dataFinal);
    }

    static String formatarPrazo(LocalDate dataLimite) {
        return dataLimite != null ? DateFormatter.format(dataLimite) : "Sem prazo";
    }
}
