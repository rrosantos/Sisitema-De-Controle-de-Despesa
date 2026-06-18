package br.com.controledespesas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Transporta valores consolidados para exibicao de Dashboard.
 */
public record DashboardResumo(
        LocalDate dataInicial,
        LocalDate dataFinal,
        BigDecimal saldoTotal,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal resultadoPeriodo,
        int contasAtivas,
        int transacoesPendentes,
        List<ResumoContaDashboard> contas,
        List<ResumoCategoriaDashboard> despesasPorCategoria,
        List<TransacaoRecenteDashboard> transacoesRecentes,
        List<ResumoCofrinhoDashboard> cofrinhos
) {

    public DashboardResumo {
        saldoTotal = saldoTotal != null ? saldoTotal : BigDecimal.ZERO;
        totalReceitas = totalReceitas != null ? totalReceitas : BigDecimal.ZERO;
        totalDespesas = totalDespesas != null ? totalDespesas : BigDecimal.ZERO;
        resultadoPeriodo = resultadoPeriodo != null ? resultadoPeriodo : BigDecimal.ZERO;
        contas = contas != null ? List.copyOf(contas) : List.of();
        despesasPorCategoria = despesasPorCategoria != null ? List.copyOf(despesasPorCategoria) : List.of();
        transacoesRecentes = transacoesRecentes != null ? List.copyOf(transacoesRecentes) : List.of();
        cofrinhos = cofrinhos != null ? List.copyOf(cofrinhos) : List.of();
    }
}
