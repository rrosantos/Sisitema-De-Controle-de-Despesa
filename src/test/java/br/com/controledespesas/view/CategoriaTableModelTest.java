package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CategoriaTableModelTest {

    @Test
    void shouldExposeExpectedColumnsAndFormattedValues() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Mercado");
        categoria.setTipo(TipoCategoria.DESPESA);
        categoria.setDescricao(null);
        categoria.setAtivo(false);

        CategoriaTableModel tableModel = new CategoriaTableModel();
        tableModel.atualizarCategorias(List.of(categoria));

        assertEquals(1, tableModel.getRowCount());
        assertEquals(5, tableModel.getColumnCount());
        assertEquals("Nome", tableModel.getColumnName(0));
        assertEquals("Tipo", tableModel.getColumnName(1));
        assertEquals("Descricao", tableModel.getColumnName(2));
        assertEquals("Status", tableModel.getColumnName(3));
        assertEquals("Acoes", tableModel.getColumnName(4));
        assertEquals("Mercado", tableModel.getValueAt(0, 0));
        assertEquals("Despesa", tableModel.getValueAt(0, 1));
        assertEquals("-", tableModel.getValueAt(0, 2));
        assertEquals("Inativa", tableModel.getValueAt(0, 3));
        assertEquals("Acoes", tableModel.getValueAt(0, 4));
        assertFalse(tableModel.isCellEditable(0, 0));
    }

    @Test
    void shouldReplaceDataWhenUpdatingCategories() {
        CategoriaTableModel tableModel = new CategoriaTableModel();

        tableModel.atualizarCategorias(List.of(categoria("Salario", TipoCategoria.RECEITA)));
        assertEquals(1, tableModel.getRowCount());

        tableModel.atualizarCategorias(List.of(
                categoria("Lazer", TipoCategoria.DESPESA),
                categoria("Investimentos", TipoCategoria.RECEITA)
        ));

        assertEquals(2, tableModel.getRowCount());
        assertEquals("Lazer", tableModel.getValueAt(0, 0));
        assertEquals("Investimentos", tableModel.getValueAt(1, 0));
    }

    private Categoria categoria(String nome, TipoCategoria tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setAtivo(true);
        return categoria;
    }
}
