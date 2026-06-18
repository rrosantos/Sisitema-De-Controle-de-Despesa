package br.com.controledespesas.view.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transporta dados capturados no formulario de CofrinhoForm.
 */
public record DadosCofrinhoForm(
        String nome,
        String descricao,
        BigDecimal valorMeta,
        LocalDate dataLimite
) {
}
