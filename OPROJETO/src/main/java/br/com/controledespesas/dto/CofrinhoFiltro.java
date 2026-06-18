package br.com.controledespesas.dto;

import br.com.controledespesas.model.StatusCofrinho;

/**
 * Transporta criterios de filtro para consultas de Cofrinho.
 */
public record CofrinhoFiltro(
        String pesquisa,
        StatusCofrinho status,
        PrazoCofrinhoFiltro prazo
) {

    public CofrinhoFiltro {
        if (pesquisa != null) {
            pesquisa = pesquisa.trim();
            if (pesquisa.isBlank()) {
                pesquisa = null;
            }
        }

        if (prazo == null) {
            prazo = PrazoCofrinhoFiltro.TODOS;
        }
    }

    public CofrinhoFiltro() {
        this(null, null, PrazoCofrinhoFiltro.TODOS);
    }

    public boolean possuiFiltros() {
        return pesquisa != null
                || status != null
                || prazo != PrazoCofrinhoFiltro.TODOS;
    }
}
