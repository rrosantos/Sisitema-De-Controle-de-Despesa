package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transporta dados capturados no formulario de TransacaoForm.
 */
public record DadosTransacaoForm(
        TipoTransacao tipo,
        String descricao,
        BigDecimal valor,
        LocalDate dataTransacao,
        Long categoriaId,
        Long contaId,
        StatusTransacao status,
        String observacoes
) {
}
