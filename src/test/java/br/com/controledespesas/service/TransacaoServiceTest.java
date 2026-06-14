package br.com.controledespesas.service;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
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
class TransacaoServiceTest {

    @Mock
    private TransacaoDAO transacaoDAO;

    @Mock
    private CategoriaDAO categoriaDAO;

    @Mock
    private ContaDAO contaDAO;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    private TransacaoService transacaoService;

    @BeforeEach
    void setUp() {
        transacaoService = new TransacaoService(transacaoDAO, categoriaDAO, contaDAO, connectionProvider);
    }

    @Test
    void shouldRegisterValidRevenueTransaction() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.RECEITA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.of(contaAtiva()));
        when(transacaoDAO.inserir(eq(connection), any(Transacao.class))).thenAnswer(invocation -> {
            Transacao transacao = invocation.getArgument(1);
            transacao.setId(100L);
            return 100L;
        });

        Transacao transacao = transacaoService.cadastrar(
                1L,
                10L,
                20L,
                TipoTransacao.RECEITA,
                "Salario",
                new BigDecimal("2500.00"),
                LocalDate.of(2026, 6, 14),
                StatusTransacao.RECEBIDO,
                "Junho"
        );

        assertEquals(100L, transacao.getId());
        verify(connection).commit();
    }

    @Test
    void shouldRegisterValidExpenseTransaction() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 11L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.DESPESA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.of(contaAtiva()));
        when(transacaoDAO.inserir(eq(connection), any(Transacao.class))).thenAnswer(invocation -> {
            Transacao transacao = invocation.getArgument(1);
            transacao.setId(101L);
            return 101L;
        });

        Transacao transacao = transacaoService.cadastrar(
                1L,
                11L,
                20L,
                TipoTransacao.DESPESA,
                "Mercado",
                new BigDecimal("300.00"),
                LocalDate.of(2026, 6, 14),
                StatusTransacao.PAGO,
                null
        );

        assertEquals(101L, transacao.getId());
        verify(connection).commit();
    }

    @Test
    void shouldRejectNonPositiveTransactionValue() {
        assertThrows(ValidacaoException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        BigDecimal.ZERO,
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectCategoryFromAnotherUser() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectAccountFromAnotherUser() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.RECEITA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectInactiveCategory() throws SQLException {
        prepararConexaoTransacional();
        Categoria categoria = categoriaAtiva(TipoCategoria.RECEITA);
        categoria.setAtivo(false);
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoria));

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectInactiveAccount() throws SQLException {
        prepararConexaoTransacional();
        Conta conta = contaAtiva();
        conta.setAtivo(false);
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.RECEITA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.of(conta));

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectIncompatibleCategoryType() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.DESPESA)));

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectRevenueWithPaidStatus() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.RECEITA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.of(contaAtiva()));

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.RECEITA,
                        "Salario",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.PAGO,
                        null
                ));
    }

    @Test
    void shouldRejectExpenseWithReceivedStatus() throws SQLException {
        prepararConexaoTransacional();
        when(categoriaDAO.buscarPorId(connection, 10L, 1L)).thenReturn(Optional.of(categoriaAtiva(TipoCategoria.DESPESA)));
        when(contaDAO.buscarPorId(connection, 20L, 1L)).thenReturn(Optional.of(contaAtiva()));

        assertThrows(RegraNegocioException.class,
                () -> transacaoService.cadastrar(
                        1L,
                        10L,
                        20L,
                        TipoTransacao.DESPESA,
                        "Mercado",
                        new BigDecimal("100.00"),
                        LocalDate.now(),
                        StatusTransacao.RECEBIDO,
                        null
                ));
    }

    @Test
    void shouldRejectInvertedDateRange() {
        assertThrows(ValidacaoException.class,
                () -> transacaoService.filtrar(
                        1L,
                        new TransacaoFiltro(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 14),
                                null, null, null, null, null)
                ));
    }

    @Test
    void shouldCalculatePeriodBalance() throws SQLException {
        when(transacaoDAO.calcularTotalReceitas(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(new BigDecimal("1000.00"));
        when(transacaoDAO.calcularTotalDespesas(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(new BigDecimal("400.00"));

        BigDecimal saldo = transacaoService.calcularSaldoDoPeriodo(
                1L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertEquals(new BigDecimal("600.00"), saldo);
    }

    private Categoria categoriaAtiva(TipoCategoria tipo) {
        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setUsuarioId(1L);
        categoria.setNome("Categoria");
        categoria.setTipo(tipo);
        categoria.setAtivo(true);
        return categoria;
    }

    private Conta contaAtiva() {
        Conta conta = new Conta();
        conta.setId(20L);
        conta.setUsuarioId(1L);
        conta.setNome("Conta Principal");
        conta.setTipo(TipoConta.CONTA_CORRENTE);
        conta.setSaldoInicial(new BigDecimal("0.00"));
        conta.setAtivo(true);
        return conta;
    }

    private void prepararConexaoTransacional() throws SQLException {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
    }
}
