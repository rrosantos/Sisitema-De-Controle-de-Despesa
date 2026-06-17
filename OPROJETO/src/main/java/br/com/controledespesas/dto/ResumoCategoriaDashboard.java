package br.com.controledespesas.dto;

import java.math.BigDecimal;

public record ResumoCategoriaDashboard(
        Long categoriaId,
        String nomeCategoria,
        BigDecimal total,
        BigDecimal percentual
) {

    public ResumoCategoriaDashboard {
        total = total != null ? total : BigDecimal.ZERO;
        percentual = percentual != null ? percentual : BigDecimal.ZERO;
    }
}
