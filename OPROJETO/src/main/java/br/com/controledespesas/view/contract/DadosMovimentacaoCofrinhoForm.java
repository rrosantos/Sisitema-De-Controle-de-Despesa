package br.com.controledespesas.view.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosMovimentacaoCofrinhoForm(
        BigDecimal valor,
        LocalDate dataMovimentacao,
        String observacao
) {
}
