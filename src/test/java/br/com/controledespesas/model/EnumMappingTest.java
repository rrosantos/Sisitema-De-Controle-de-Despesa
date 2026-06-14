package br.com.controledespesas.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumMappingTest {

    @Test
    void shouldConvertAllEnumsToDatabaseValues() {
        assertAll(
                () -> assertEquals("receita", TipoCategoria.RECEITA.getValorBanco()),
                () -> assertEquals("despesa", TipoCategoria.DESPESA.getValorBanco()),
                () -> assertEquals("carteira", TipoConta.CARTEIRA.getValorBanco()),
                () -> assertEquals("conta_corrente", TipoConta.CONTA_CORRENTE.getValorBanco()),
                () -> assertEquals("poupanca", TipoConta.POUPANCA.getValorBanco()),
                () -> assertEquals("conta_digital", TipoConta.CONTA_DIGITAL.getValorBanco()),
                () -> assertEquals("outro", TipoConta.OUTRO.getValorBanco()),
                () -> assertEquals("receita", TipoTransacao.RECEITA.getValorBanco()),
                () -> assertEquals("despesa", TipoTransacao.DESPESA.getValorBanco()),
                () -> assertEquals("pendente", StatusTransacao.PENDENTE.getValorBanco()),
                () -> assertEquals("pago", StatusTransacao.PAGO.getValorBanco()),
                () -> assertEquals("recebido", StatusTransacao.RECEBIDO.getValorBanco()),
                () -> assertEquals("cancelado", StatusTransacao.CANCELADO.getValorBanco()),
                () -> assertEquals("em_andamento", StatusCofrinho.EM_ANDAMENTO.getValorBanco()),
                () -> assertEquals("concluido", StatusCofrinho.CONCLUIDO.getValorBanco()),
                () -> assertEquals("cancelado", StatusCofrinho.CANCELADO.getValorBanco()),
                () -> assertEquals("deposito", TipoMovimentacaoCofrinho.DEPOSITO.getValorBanco()),
                () -> assertEquals("retirada", TipoMovimentacaoCofrinho.RETIRADA.getValorBanco())
        );
    }

    @Test
    void shouldConvertDatabaseValuesToEnumsIgnoringCase() {
        assertAll(
                () -> assertEquals(TipoCategoria.RECEITA, TipoCategoria.fromValorBanco("RECEITA")),
                () -> assertEquals(TipoConta.CONTA_CORRENTE, TipoConta.fromValorBanco("Conta_Corrente")),
                () -> assertEquals(TipoTransacao.DESPESA, TipoTransacao.fromValorBanco("despesa")),
                () -> assertEquals(StatusTransacao.RECEBIDO, StatusTransacao.fromValorBanco("RECEBIDO")),
                () -> assertEquals(StatusCofrinho.EM_ANDAMENTO, StatusCofrinho.fromValorBanco("Em_Andamento")),
                () -> assertEquals(TipoMovimentacaoCofrinho.RETIRADA, TipoMovimentacaoCofrinho.fromValorBanco("Retirada"))
        );
    }

    @Test
    void shouldRejectInvalidDatabaseValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> TipoCategoria.fromValorBanco("outro")),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoConta.fromValorBanco("investimento")),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoTransacao.fromValorBanco("transferencia")),
                () -> assertThrows(IllegalArgumentException.class, () -> StatusTransacao.fromValorBanco("agendado")),
                () -> assertThrows(IllegalArgumentException.class, () -> StatusCofrinho.fromValorBanco("pausado")),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoMovimentacaoCofrinho.fromValorBanco("aporte"))
        );
    }

    @Test
    void shouldRejectNullDatabaseValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> TipoCategoria.fromValorBanco(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoConta.fromValorBanco(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoTransacao.fromValorBanco(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> StatusTransacao.fromValorBanco(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> StatusCofrinho.fromValorBanco(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> TipoMovimentacaoCofrinho.fromValorBanco(null))
        );
    }
}
