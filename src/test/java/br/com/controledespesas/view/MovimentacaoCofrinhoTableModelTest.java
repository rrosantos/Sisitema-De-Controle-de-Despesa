package br.com.controledespesas.view;

import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MovimentacaoCofrinhoTableModelTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldExposeFormattedHistoryValues() {
        MovimentacaoCofrinhoTableModel tableModel = new MovimentacaoCofrinhoTableModel(moneyFormatter);
        tableModel.atualizarMovimentacoes(List.of(movimentacao(
                TipoMovimentacaoCofrinho.DEPOSITO,
                new BigDecimal("250.50"),
                LocalDate.of(2026, 6, 14),
                "Aporte mensal"
        )));

        assertEquals(1, tableModel.getRowCount());
        assertEquals(4, tableModel.getColumnCount());
        assertEquals("Data", tableModel.getColumnName(0));
        assertEquals("Tipo", tableModel.getColumnName(1));
        assertEquals("Valor", tableModel.getColumnName(2));
        assertEquals("Observacao", tableModel.getColumnName(3));
        assertEquals("14/06/2026", tableModel.getValueAt(0, 0));
        assertEquals("Deposito", tableModel.getValueAt(0, 1));
        assertEquals(moneyFormatter.format(new BigDecimal("250.50")), tableModel.getValueAt(0, 2));
        assertEquals("Aporte mensal", tableModel.getValueAt(0, 3));
        assertFalse(tableModel.isCellEditable(0, 0));
    }

    @Test
    void shouldShowDashWhenObservationIsMissingAndReplaceRows() {
        MovimentacaoCofrinhoTableModel tableModel = new MovimentacaoCofrinhoTableModel(moneyFormatter);
        tableModel.atualizarMovimentacoes(List.of(movimentacao(
                TipoMovimentacaoCofrinho.RETIRADA,
                new BigDecimal("20.00"),
                LocalDate.of(2026, 6, 15),
                " "
        )));

        assertEquals("-", tableModel.getValueAt(0, 3));

        tableModel.atualizarMovimentacoes(List.of(
                movimentacao(TipoMovimentacaoCofrinho.DEPOSITO, new BigDecimal("10.00"), LocalDate.of(2026, 6, 10), null),
                movimentacao(TipoMovimentacaoCofrinho.RETIRADA, new BigDecimal("5.00"), LocalDate.of(2026, 6, 11), "Cafe")
        ));

        assertEquals(2, tableModel.getRowCount());
        assertEquals("Cafe", tableModel.getValueAt(1, 3));
    }

    private MovimentacaoCofrinho movimentacao(TipoMovimentacaoCofrinho tipo, BigDecimal valor,
                                              LocalDate dataMovimentacao, String observacao) {
        MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho();
        movimentacao.setTipo(tipo);
        movimentacao.setValor(valor);
        movimentacao.setDataMovimentacao(dataMovimentacao);
        movimentacao.setObservacao(observacao);
        return movimentacao;
    }
}
