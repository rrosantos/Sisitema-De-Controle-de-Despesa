package br.com.controledespesas.view.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transporta dados capturados no formulario de MovimentacaoCofrinhoForm.
 */
public record DadosMovimentacaoCofrinhoForm(
        BigDecimal valor,
        LocalDate dataMovimentacao,
        String observacao
) {
}
