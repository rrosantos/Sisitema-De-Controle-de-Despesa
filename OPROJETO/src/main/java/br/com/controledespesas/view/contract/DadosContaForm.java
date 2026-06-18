package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;

/**
 * Transporta dados capturados no formulario de ContaForm.
 */
public record DadosContaForm(
        String nome,
        TipoConta tipo,
        String instituicao,
        BigDecimal saldoInicial
) {
}
