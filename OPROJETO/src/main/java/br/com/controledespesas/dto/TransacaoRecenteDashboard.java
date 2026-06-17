package br.com.controledespesas.dto;

import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRecenteDashboard(
        Long id,
        LocalDate data,
        String descricao,
        TipoTransacao tipo,
        StatusTransacao status,
        BigDecimal valor,
        String categoria,
        String conta
) {

    public TransacaoRecenteDashboard {
        valor = valor != null ? valor : BigDecimal.ZERO;
    }
}
