package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.TipoCategoria;

/**
 * Transporta dados capturados no formulario de CategoriaForm.
 */
public record DadosCategoriaForm(
        String nome,
        TipoCategoria tipo,
        String descricao
) {
}
