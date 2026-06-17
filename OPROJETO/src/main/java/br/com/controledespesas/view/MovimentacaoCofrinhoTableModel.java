package br.com.controledespesas.view;

import br.com.controledespesas.model.MovimentacaoCofrinho;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

class MovimentacaoCofrinhoTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {"Data", "Tipo", "Valor", "Observacao"};

    private final MoneyFormatter moneyFormatter;
    private final List<MovimentacaoCofrinho> movimentacoes = new ArrayList<>();

    MovimentacaoCofrinhoTableModel(MoneyFormatter moneyFormatter) {
        this.moneyFormatter = moneyFormatter;
    }

    @Override
    public int getRowCount() {
        return movimentacoes.size();
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
        MovimentacaoCofrinho movimentacao = movimentacoes.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> DateFormatter.format(movimentacao.getDataMovimentacao());
            case 1 -> ViewFormatters.formatTipoMovimentacaoCofrinho(movimentacao.getTipo());
            case 2 -> moneyFormatter.format(movimentacao.getValor());
            case 3 -> movimentacao.getObservacao() != null && !movimentacao.getObservacao().isBlank()
                    ? movimentacao.getObservacao()
                    : "-";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    void atualizarMovimentacoes(List<MovimentacaoCofrinho> novasMovimentacoes) {
        movimentacoes.clear();
        if (novasMovimentacoes != null) {
            movimentacoes.addAll(novasMovimentacoes);
        }
        fireTableDataChanged();
    }

    MovimentacaoCofrinho getMovimentacaoAt(int rowIndex) {
        return movimentacoes.get(rowIndex);
    }
}
