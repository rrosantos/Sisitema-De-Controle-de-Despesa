package br.com.controledespesas.view;

import br.com.controledespesas.dto.TransacaoRecenteDashboard;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TransacaoRecenteDashboardTableModelTest {

    @Test
    void shouldExposeColumnsAndFormattedValues() {
        TransacaoRecenteDashboardTableModel tableModel = new TransacaoRecenteDashboardTableModel();
        tableModel.atualizarTransacoes(List.of(new TransacaoRecenteDashboard(
                1L,
                LocalDate.of(2026, 6, 15),
                "Mercado",
                TipoTransacao.DESPESA,
                StatusTransacao.PAGO,
                new BigDecimal("320.45"),
                "Alimentacao",
                "Carteira"
        )));

        assertEquals(6, tableModel.getColumnCount());
        assertEquals("Data", tableModel.getColumnName(0));
        assertEquals("Descricao", tableModel.getColumnName(1));
        assertEquals("Categoria", tableModel.getColumnName(2));
        assertEquals("Tipo", tableModel.getColumnName(3));
        assertEquals("Valor", tableModel.getColumnName(4));
        assertEquals("Status", tableModel.getColumnName(5));
        assertEquals("15/06/2026", tableModel.getValueAt(0, 0));
        assertEquals("Mercado", tableModel.getValueAt(0, 1));
        assertEquals("Alimentacao", tableModel.getValueAt(0, 2));
        assertEquals("Despesa", tableModel.getValueAt(0, 3));
        assertEquals(new MoneyFormatter().format(new BigDecimal("320.45")), tableModel.getValueAt(0, 4));
        assertEquals("Pago", tableModel.getValueAt(0, 5));
        assertFalse(tableModel.isCellEditable(0, 0));
    }

    @Test
    void shouldHandleMissingCategoryAndEmptyList() {
        TransacaoRecenteDashboardTableModel tableModel = new TransacaoRecenteDashboardTableModel();
        tableModel.atualizarTransacoes(List.of(new TransacaoRecenteDashboard(
                2L,
                LocalDate.of(2026, 6, 14),
                "Freela",
                TipoTransacao.RECEITA,
                StatusTransacao.RECEBIDO,
                new BigDecimal("800.00"),
                null,
                "Banco"
        )));

        assertEquals("-", tableModel.getValueAt(0, 2));

        tableModel.atualizarTransacoes(List.of());

        assertEquals(0, tableModel.getRowCount());
    }
}
