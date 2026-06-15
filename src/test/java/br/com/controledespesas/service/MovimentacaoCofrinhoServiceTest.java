package br.com.controledespesas.service;

import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimentacaoCofrinhoServiceTest {

    @Mock
    private MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;

    @Mock
    private CofrinhoDAO cofrinhoDAO;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    private MovimentacaoCofrinhoService movimentacaoCofrinhoService;

    @BeforeEach
    void setUp() {
        movimentacaoCofrinhoService =
                new MovimentacaoCofrinhoService(movimentacaoCofrinhoDAO, cofrinhoDAO, connectionProvider);
    }

    @Test
    void shouldDepositSuccessfully() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoEmAndamento()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("100.00")));
        when(movimentacaoCofrinhoDAO.inserir(eq(connection), any(MovimentacaoCofrinho.class))).thenAnswer(invocation -> {
            MovimentacaoCofrinho movimentacao = invocation.getArgument(1);
            movimentacao.setId(40L);
            return 40L;
        });

        MovimentacaoCofrinho movimentacao = movimentacaoCofrinhoService.depositar(
                30L,
                1L,
                new BigDecimal("50.00"),
                LocalDate.of(2026, 6, 14),
                "Aporte"
        );

        assertEquals(40L, movimentacao.getId());
        assertEquals(TipoMovimentacaoCofrinho.DEPOSITO, movimentacao.getTipo());
        verify(connection).commit();
    }

    @Test
    void shouldWithdrawSuccessfully() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoEmAndamento()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("300.00")));
        when(movimentacaoCofrinhoDAO.inserir(eq(connection), any(MovimentacaoCofrinho.class))).thenAnswer(invocation -> {
            MovimentacaoCofrinho movimentacao = invocation.getArgument(1);
            movimentacao.setId(41L);
            return 41L;
        });

        MovimentacaoCofrinho movimentacao = movimentacaoCofrinhoService.retirar(
                30L,
                1L,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 6, 14),
                "Uso"
        );

        assertEquals(TipoMovimentacaoCofrinho.RETIRADA, movimentacao.getTipo());
        verify(connection).commit();
    }

    @Test
    void shouldRejectWithdrawalGreaterThanCurrentBalance() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoEmAndamento()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("50.00")));

        RegraNegocioException exception = assertThrows(
                RegraNegocioException.class,
                () -> movimentacaoCofrinhoService.retirar(
                        30L,
                        1L,
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 6, 14),
                        "Uso"
                )
        );

        assertEquals(
                "O valor da retirada nao pode ser maior que o valor disponivel no cofrinho.",
                exception.getMessage()
        );
        verify(movimentacaoCofrinhoDAO, never()).inserir(eq(connection), any(MovimentacaoCofrinho.class));
    }

    @Test
    void shouldRejectMovementForCanceledSavingsGoal() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoCancelado()));

        assertThrows(RegraNegocioException.class,
                () -> movimentacaoCofrinhoService.depositar(
                        30L,
                        1L,
                        new BigDecimal("10.00"),
                        LocalDate.of(2026, 6, 14),
                        null
                ));
    }

    @Test
    void shouldMarkSavingsGoalAsCompletedWhenDepositReachesTarget() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoEmAndamento()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("450.00")));
        when(movimentacaoCofrinhoDAO.inserir(eq(connection), any(MovimentacaoCofrinho.class))).thenReturn(42L);

        movimentacaoCofrinhoService.depositar(
                30L,
                1L,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 6, 14),
                "Aporte"
        );

        verify(cofrinhoDAO).atualizarStatus(connection, 30L, 1L, StatusCofrinho.CONCLUIDO);
    }

    @Test
    void shouldReturnSavingsGoalToInProgressWhenWithdrawalDropsBelowTarget() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoConcluido()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("600.00")));
        when(movimentacaoCofrinhoDAO.inserir(eq(connection), any(MovimentacaoCofrinho.class))).thenReturn(43L);

        movimentacaoCofrinhoService.retirar(
                30L,
                1L,
                new BigDecimal("200.00"),
                LocalDate.of(2026, 6, 14),
                "Uso"
        );

        verify(cofrinhoDAO).atualizarStatus(connection, 30L, 1L, StatusCofrinho.EM_ANDAMENTO);
    }

    @Test
    void shouldRejectMovementForSavingsGoalFromAnotherUser() throws SQLException {
        prepararConexaoTransacional();
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> movimentacaoCofrinhoService.depositar(
                        30L,
                        1L,
                        new BigDecimal("10.00"),
                        LocalDate.of(2026, 6, 14),
                        null
                ));
    }

    @Test
    void shouldRejectNonPositiveMovementValue() {
        assertThrows(ValidacaoException.class,
                () -> movimentacaoCofrinhoService.depositar(
                        30L,
                        1L,
                        BigDecimal.ZERO,
                        LocalDate.of(2026, 6, 14),
                        null
                ));
    }

    @Test
    void shouldBlockDeletingDepositWhenItWouldMakeBalanceNegative() throws SQLException {
        prepararConexaoTransacional();
        MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho();
        movimentacao.setId(50L);
        movimentacao.setCofrinhoId(30L);
        movimentacao.setUsuarioId(1L);
        movimentacao.setTipo(TipoMovimentacaoCofrinho.DEPOSITO);
        movimentacao.setValor(new BigDecimal("100.00"));

        when(movimentacaoCofrinhoDAO.buscarPorId(connection, 50L, 1L)).thenReturn(Optional.of(movimentacao));
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinhoEmAndamento()));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("50.00")));

        assertThrows(RegraNegocioException.class,
                () -> movimentacaoCofrinhoService.excluir(50L, 1L));

        verify(movimentacaoCofrinhoDAO, never()).excluir(connection, 50L, 1L);
    }

    private Cofrinho cofrinhoEmAndamento() {
        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setId(30L);
        cofrinho.setUsuarioId(1L);
        cofrinho.setNome("Reserva");
        cofrinho.setValorMeta(new BigDecimal("500.00"));
        cofrinho.setStatus(StatusCofrinho.EM_ANDAMENTO);
        return cofrinho;
    }

    private Cofrinho cofrinhoConcluido() {
        Cofrinho cofrinho = cofrinhoEmAndamento();
        cofrinho.setStatus(StatusCofrinho.CONCLUIDO);
        return cofrinho;
    }

    private Cofrinho cofrinhoCancelado() {
        Cofrinho cofrinho = cofrinhoEmAndamento();
        cofrinho.setStatus(StatusCofrinho.CANCELADO);
        return cofrinho;
    }

    private void prepararConexaoTransacional() throws SQLException {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
    }
}
