package br.com.controledespesas.dto;

import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;

import java.time.LocalDate;

/**
 * Transporta criterios de filtro para consultas de Transacao.
 */
public record TransacaoFiltro(
        LocalDate dataInicial,
        LocalDate dataFinal,
        TipoTransacao tipo,
        StatusTransacao status,
        Long categoriaId,
        Long contaId,
        String descricao
) {

    public TransacaoFiltro {
        if (descricao != null) {
            descricao = descricao.trim();
            if (descricao.isBlank()) {
                descricao = null;
            }
        }
    }

    public TransacaoFiltro() {
        this(null, null, null, null, null, null, null);
    }

    public boolean possuiFiltros() {
        return dataInicial != null
                || dataFinal != null
                || tipo != null
                || status != null
                || categoriaId != null
                || contaId != null
                || descricao != null;
    }
}
