package br.com.controledespesas.dto;

import br.com.controledespesas.model.StatusCofrinho;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoCofrinhoDashboard(
        Long cofrinhoId,
        String nome,
        StatusCofrinho status,
        BigDecimal valorAtual,
        BigDecimal valorMeta,
        BigDecimal percentual,
        LocalDate dataLimite
) {

    public ResumoCofrinhoDashboard {
        valorAtual = valorAtual != null ? valorAtual : BigDecimal.ZERO;
        valorMeta = valorMeta != null ? valorMeta : BigDecimal.ZERO;
        percentual = percentual != null ? percentual : BigDecimal.ZERO;
    }
}
