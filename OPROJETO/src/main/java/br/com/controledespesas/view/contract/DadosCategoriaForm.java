package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.TipoCategoria;

public record DadosCategoriaForm(
        String nome,
        TipoCategoria tipo,
        String descricao
) {
}
