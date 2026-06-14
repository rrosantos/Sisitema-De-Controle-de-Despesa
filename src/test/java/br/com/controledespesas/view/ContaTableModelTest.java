package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContaTableModelTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldExposeExpectedColumnsAndFormattedValues() {
        Conta conta = conta(1L, "Banco", TipoConta.CONTA_CORRENTE, "Banco Azul", new BigDecimal("1250.75"), true);
        ContaTableModel tableModel = new ContaTableModel(moneyFormatter);
        tableModel.atualizarContas(List.of(conta));
        tableModel.atualizarSaldos(Map.of(1L, new BigDecimal("1500.90")));

        assertEquals(1, tableModel.getRowCount());
        assertEquals(7, tableModel.getColumnCount());
        assertEquals("Nome", tableModel.getColumnName(0));
        assertEquals("Tipo", tableModel.getColumnName(1));
        assertEquals("Instituicao", tableModel.getColumnName(2));
        assertEquals("Saldo inicial", tableModel.getColumnName(3));
        assertEquals("Saldo atual", tableModel.getColumnName(4));
        assertEquals("Status", tableModel.getColumnName(5));
        assertEquals("Acoes", tableModel.getColumnName(6));
        assertEquals("Banco", tableModel.getValueAt(0, 0));
        assertEquals("Conta-corrente", tableModel.getValueAt(0, 1));
        assertEquals("Banco Azul", tableModel.getValueAt(0, 2));
        assertEquals(moneyFormatter.format(new BigDecimal("1250.75")), tableModel.getValueAt(0, 3));
        assertEquals(moneyFormatter.format(new BigDecimal("1500.90")), tableModel.getValueAt(0, 4));
        assertEquals("Ativa", tableModel.getValueAt(0, 5));
        assertEquals("Acoes", tableModel.getValueAt(0, 6));
        assertFalse(tableModel.isCellEditable(0, 0));
    }

    @Test
    void shouldShowDashWhenInstitutionOrBalanceIsMissing() {
        Conta conta = conta(2L, "Carteira", TipoConta.CARTEIRA, null, BigDecimal.ZERO.setScale(2), false);
        ContaTableModel tableModel = new ContaTableModel(moneyFormatter);
        tableModel.atualizarContas(List.of(conta));

        assertEquals("-", tableModel.getValueAt(0, 2));
        assertEquals("-", tableModel.getValueAt(0, 4));
        assertEquals("Inativa", tableModel.getValueAt(0, 5));
    }

    @Test
    void shouldReplaceRowsWhenUpdatingAccounts() {
        ContaTableModel tableModel = new ContaTableModel(moneyFormatter);
        tableModel.atualizarContas(List.of(conta(1L, "Carteira", TipoConta.CARTEIRA, null, BigDecimal.ZERO, true)));
        assertEquals(1, tableModel.getRowCount());

        tableModel.atualizarContas(List.of(
                conta(2L, "Banco", TipoConta.CONTA_CORRENTE, "Banco Azul", BigDecimal.TEN, true),
                conta(3L, "Reserva", TipoConta.POUPANCA, null, BigDecimal.ONE, true)
        ));

        assertEquals(2, tableModel.getRowCount());
        assertEquals("Banco", tableModel.getValueAt(0, 0));
        assertEquals("Reserva", tableModel.getValueAt(1, 0));
    }

    private Conta conta(Long id, String nome, TipoConta tipo, String instituicao, BigDecimal saldoInicial, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setNome(nome);
        conta.setTipo(tipo);
        conta.setInstituicao(instituicao);
        conta.setSaldoInicial(saldoInicial.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }
}
