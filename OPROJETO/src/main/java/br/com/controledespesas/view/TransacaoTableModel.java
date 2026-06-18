package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.Transacao;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapta dados de Transacao para exibicao em tabela Swing.
 */
class TransacaoTableModel extends AbstractTableModel {

    private static final String[] COLUNAS = {
            "Data", "Descricao", "Tipo", "Categoria", "Conta", "Valor", "Status", "Acoes"
    };

    private final MoneyFormatter moneyFormatter;
    private final List<Transacao> transacoes = new ArrayList<>();
    private final Map<Long, Categoria> categorias = new HashMap<>();
    private final Map<Long, Conta> contas = new HashMap<>();

    TransacaoTableModel(MoneyFormatter moneyFormatter) {
        this.moneyFormatter = moneyFormatter;
    }

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
        Transacao transacao = transacoes.get(rowIndex);
        Categoria categoria = categorias.get(transacao.getCategoriaId());
        Conta conta = contas.get(transacao.getContaId());

        return switch (columnIndex) {
            case 0 -> DateFormatter.format(transacao.getDataTransacao());
            case 1 -> transacao.getDescricao();
            case 2 -> ViewFormatters.formatTipoTransacao(transacao.getTipo());
            case 3 -> categoria != null ? categoria.getNome() : ViewFormatters.formatOptionalText(null);
            case 4 -> conta != null ? conta.getNome() : ViewFormatters.formatOptionalText(null);
            case 5 -> moneyFormatter.format(transacao.getValor());
            case 6 -> ViewFormatters.formatStatusTransacao(transacao.getStatus());
            case 7 -> "Acoes";
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    void atualizarTransacoes(List<Transacao> novasTransacoes) {
        transacoes.clear();
        if (novasTransacoes != null) {
            transacoes.addAll(novasTransacoes);
        }
        fireTableDataChanged();
    }

    void atualizarDadosRelacionados(Map<Long, Categoria> novasCategorias, Map<Long, Conta> novasContas) {
        categorias.clear();
        contas.clear();

        if (novasCategorias != null) {
            categorias.putAll(novasCategorias);
        }
        if (novasContas != null) {
            contas.putAll(novasContas);
        }

        fireTableDataChanged();
    }

    Transacao getTransacaoAt(int rowIndex) {
        return transacoes.get(rowIndex);
    }
}
