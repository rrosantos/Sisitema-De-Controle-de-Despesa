package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapta dados de Conta para exibicao em tabela Swing.
 */
class ContaTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {
            "Nome", "Tipo", "Instituicao", "Saldo inicial", "Saldo atual", "Status", "Acoes"
    };

    private final MoneyFormatter moneyFormatter;
    private final List<Conta> contas = new ArrayList<>();
    private final Map<Long, BigDecimal> saldos = new HashMap<>();

    ContaTableModel(MoneyFormatter moneyFormatter) {
        this.moneyFormatter = moneyFormatter;
    }

    @Override
    public int getRowCount() {
        return contas.size();
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
        Conta conta = contas.get(rowIndex);
        BigDecimal saldoAtual = saldos.get(conta.getId());

        return switch (columnIndex) {
            case 0 -> conta.getNome();
            case 1 -> ViewFormatters.formatTipoConta(conta.getTipo());
            case 2 -> ViewFormatters.formatOptionalText(conta.getInstituicao());
            case 3 -> moneyFormatter.format(conta.getSaldoInicial());
            case 4 -> saldoAtual != null ? moneyFormatter.format(saldoAtual) : ViewFormatters.formatOptionalText(null);
            case 5 -> ViewFormatters.formatStatus(conta.isAtivo());
            case 6 -> "Acoes";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void atualizarContas(List<Conta> novasContas) {
        contas.clear();
        if (novasContas != null) {
            contas.addAll(novasContas);
        }
        fireTableDataChanged();
    }

    public void atualizarSaldos(Map<Long, BigDecimal> novosSaldos) {
        saldos.clear();
        if (novosSaldos != null) {
            saldos.putAll(novosSaldos);
        }
        fireTableDataChanged();
    }

    public Conta getContaAt(int rowIndex) {
        return contas.get(rowIndex);
    }
}
