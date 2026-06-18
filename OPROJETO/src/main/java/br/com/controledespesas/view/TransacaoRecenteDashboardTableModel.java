package br.com.controledespesas.view;

import br.com.controledespesas.dto.TransacaoRecenteDashboard;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapta dados de TransacaoRecenteDashboard para exibicao em tabela Swing.
 */
class TransacaoRecenteDashboardTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {
            "Data", "Descricao", "Categoria", "Tipo", "Valor", "Status"
    };

    private final List<TransacaoRecenteDashboard> transacoes = new ArrayList<>();

    @Override
    public int getRowCount() {
        return transacoes.size();
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
        TransacaoRecenteDashboard transacao = transacoes.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> DateFormatter.format(transacao.data());
            case 1 -> ViewFormatters.formatOptionalText(transacao.descricao());
            case 2 -> ViewFormatters.formatOptionalText(transacao.categoria());
            case 3 -> ViewFormatters.formatTipoTransacao(transacao.tipo());
            case 4 -> DashboardViewSupport.formatarMoeda(transacao.valor());
            case 5 -> ViewFormatters.formatStatusTransacao(transacao.status());
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    void atualizarTransacoes(List<TransacaoRecenteDashboard> novasTransacoes) {
        transacoes.clear();
        if (novasTransacoes != null) {
            transacoes.addAll(novasTransacoes);
        }
        fireTableDataChanged();
    }
}
