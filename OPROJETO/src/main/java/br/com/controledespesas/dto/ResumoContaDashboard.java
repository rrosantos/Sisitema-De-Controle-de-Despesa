package br.com.controledespesas.dto;

import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;

/**
 * Transporta dados resumidos de ContaDashboard para o dashboard.
 */
public record ResumoContaDashboard(
        Long contaId,
        String nome,
        TipoConta tipo,
        boolean ativa,
        BigDecimal saldoAtual
) {

    public ResumoContaDashboard {
        saldoAtual = saldoAtual != null ? saldoAtual : BigDecimal.ZERO;
    }
}
