package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

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
