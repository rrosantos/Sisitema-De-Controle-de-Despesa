package br.com.controledespesas.view.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosCofrinhoForm(
        String nome,
        String descricao,
        BigDecimal valorMeta,
        LocalDate dataLimite
) {
}
