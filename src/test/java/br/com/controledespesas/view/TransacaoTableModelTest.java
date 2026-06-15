package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TransacaoTableModelTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldExposeExpectedColumnsAndFormattedValues() {
        Transacao transacao = transacao(1L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO);
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA);
        Conta conta = conta(20L, "Banco");

        TransacaoTableModel tableModel = new TransacaoTableModel(moneyFormatter);
        tableModel.atualizarDadosRelacionados(Map.of(10L, categoria), Map.of(20L, conta));
        tableModel.atualizarTransacoes(List.of(transacao));

        assertEquals(8, tableModel.getColumnCount());
        assertEquals("Data", tableModel.getColumnName(0));
        assertEquals("Descricao", tableModel.getColumnName(1));
        assertEquals("Tipo", tableModel.getColumnName(2));
        assertEquals("Categoria", tableModel.getColumnName(3));
        assertEquals("Conta", tableModel.getColumnName(4));
        assertEquals("Valor", tableModel.getColumnName(5));
        assertEquals("Status", tableModel.getColumnName(6));
        assertEquals("Acoes", tableModel.getColumnName(7));
        assertEquals("10/06/2026", tableModel.getValueAt(0, 0));
        assertEquals("Salario", tableModel.getValueAt(0, 1));
        assertEquals("Receita", tableModel.getValueAt(0, 2));
        assertEquals("Salario", tableModel.getValueAt(0, 3));
        assertEquals("Banco", tableModel.getValueAt(0, 4));
        assertEquals(moneyFormatter.format(new BigDecimal("2500.00")), tableModel.getValueAt(0, 5));
        assertEquals("Recebido", tableModel.getValueAt(0, 6));
        assertFalse(tableModel.isCellEditable(0, 0));
    }

    @Test
    void shouldUpdateTransactionsAndFallbackToDashWhenRelatedDataIsMissing() {
        TransacaoTableModel tableModel = new TransacaoTableModel(moneyFormatter);
        tableModel.atualizarTransacoes(List.of(transacao(2L, "Mercado", TipoTransacao.DESPESA, StatusTransacao.PAGO)));

        assertEquals("-", tableModel.getValueAt(0, 3));
        assertEquals("-", tableModel.getValueAt(0, 4));

        tableModel.atualizarTransacoes(List.of(
                transacao(3L, "Freela", TipoTransacao.RECEITA, StatusTransacao.PENDENTE),
                transacao(4L, "Aluguel", TipoTransacao.DESPESA, StatusTransacao.PAGO)
        ));

        assertEquals(2, tableModel.getRowCount());
        assertEquals("Freela", tableModel.getValueAt(0, 1));
        assertEquals("Aluguel", tableModel.getValueAt(1, 1));
    }

    private Transacao transacao(Long id, String descricao, TipoTransacao tipo, StatusTransacao status) {
        Transacao transacao = new Transacao();
        transacao.setId(id);
        transacao.setCategoriaId(10L);
        transacao.setContaId(20L);
        transacao.setDescricao(descricao);
        transacao.setTipo(tipo);
        transacao.setStatus(status);
        transacao.setValor(new BigDecimal("2500.00"));
        transacao.setDataTransacao(LocalDate.of(2026, 6, 10));
        return transacao;
    }

    private Categoria categoria(Long id, String nome, TipoCategoria tipoCategoria) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setTipo(tipoCategoria);
        categoria.setAtivo(true);
        return categoria;
    }

    private Conta conta(Long id, String nome) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setNome(nome);
        conta.setTipo(TipoConta.CONTA_CORRENTE);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(true);
        return conta;
    }
}
