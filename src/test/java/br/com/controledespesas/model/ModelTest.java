package br.com.controledespesas.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {

    @Test
    void shouldCreateUsuarioModelWithoutExposingPasswordInToString() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 13, 0);
        Usuario usuario = new Usuario(1L, "Raissa", "raissa@example.com", "hash-seguro", true, now, now);

        assertAll(
                () -> assertEquals(1L, usuario.getId()),
                () -> assertEquals("Raissa", usuario.getNome()),
                () -> assertEquals("raissa@example.com", usuario.getEmail()),
                () -> assertEquals("hash-seguro", usuario.getSenhaHash()),
                () -> assertTrue(usuario.isAtivo()),
                () -> assertEquals(now, usuario.getCriadoEm()),
                () -> assertEquals(now, usuario.getAtualizadoEm()),
                () -> assertFalse(usuario.toString().contains("hash-seguro"))
        );
    }

    @Test
    void shouldCreateCategoriaAndContaModelsWithExpectedTypes() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 13, 0);
        Categoria categoria = new Categoria(2L, 1L, "Salario", TipoCategoria.RECEITA, "Pagamento mensal", true, now, now);
        Conta conta = new Conta(3L, 1L, "Conta Principal", TipoConta.CONTA_CORRENTE, "Banco X",
                new BigDecimal("1500.00"), true, now, now);

        assertAll(
                () -> assertEquals(TipoCategoria.RECEITA, categoria.getTipo()),
                () -> assertEquals("Pagamento mensal", categoria.getDescricao()),
                () -> assertEquals(TipoConta.CONTA_CORRENTE, conta.getTipo()),
                () -> assertEquals(new BigDecimal("1500.00"), conta.getSaldoInicial()),
                () -> assertEquals(now, conta.getCriadoEm()),
                () -> assertEquals(now, conta.getAtualizadoEm())
        );
    }

    @Test
    void shouldCreateTransacaoCofrinhoAndMovimentacaoModelsWithDateTypes() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 13, 0);
        LocalDate today = LocalDate.of(2026, 6, 14);

        Transacao transacao = new Transacao(4L, 1L, 2L, 3L, TipoTransacao.DESPESA, "Mercado",
                new BigDecimal("250.75"), today, StatusTransacao.PAGO, "Compra da semana", now, now);
        Cofrinho cofrinho = new Cofrinho(5L, 1L, "Reserva", "Meta anual", new BigDecimal("5000.00"),
                null, StatusCofrinho.EM_ANDAMENTO, now, now);
        MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho(6L, 5L, 1L,
                TipoMovimentacaoCofrinho.DEPOSITO, new BigDecimal("300.00"), today, "Primeiro aporte", now);

        assertAll(
                () -> assertEquals(new BigDecimal("250.75"), transacao.getValor()),
                () -> assertEquals(today, transacao.getDataTransacao()),
                () -> assertEquals(StatusTransacao.PAGO, transacao.getStatus()),
                () -> assertEquals(new BigDecimal("5000.00"), cofrinho.getValorMeta()),
                () -> assertNull(cofrinho.getDataLimite()),
                () -> assertEquals(StatusCofrinho.EM_ANDAMENTO, cofrinho.getStatus()),
                () -> assertEquals(TipoMovimentacaoCofrinho.DEPOSITO, movimentacao.getTipo()),
                () -> assertEquals(new BigDecimal("300.00"), movimentacao.getValor()),
                () -> assertEquals(today, movimentacao.getDataMovimentacao()),
                () -> assertEquals(now, movimentacao.getCriadoEm())
        );
    }
}
