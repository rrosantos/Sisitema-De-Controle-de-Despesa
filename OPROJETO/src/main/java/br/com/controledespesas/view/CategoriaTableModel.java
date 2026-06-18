package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapta dados de Categoria para exibicao em tabela Swing.
 */
class CategoriaTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {"Nome", "Tipo", "Descricao", "Status", "Acoes"};

    private final List<Categoria> categorias = new ArrayList<>();

    @Override
    public int getRowCount() {
        return categorias.size();
    }

    @Override
    public int getColumnCount() {
        return COLUNAS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUNAS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Categoria categoria = categorias.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> categoria.getNome();
            case 1 -> ViewFormatters.formatTipoCategoria(categoria.getTipo());
            case 2 -> ViewFormatters.formatOptionalText(categoria.getDescricao());
            case 3 -> ViewFormatters.formatStatus(categoria.isAtivo());
            case 4 -> "Acoes";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void atualizarCategorias(List<Categoria> novasCategorias) {
        categorias.clear();
        if (novasCategorias != null) {
            categorias.addAll(novasCategorias);
        }
        fireTableDataChanged();
    }

    public Categoria getCategoriaAt(int rowIndex) {
        return categorias.get(rowIndex);
    }
}
