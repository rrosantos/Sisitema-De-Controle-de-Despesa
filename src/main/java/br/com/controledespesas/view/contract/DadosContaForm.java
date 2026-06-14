package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;

public record DadosContaForm(
        String nome,
        TipoConta tipo,
        String instituicao,
        BigDecimal saldoInicial
) {
}
