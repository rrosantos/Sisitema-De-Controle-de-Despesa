package br.com.controledespesas.service;

import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaDAO contaDAO;

    private ContaService contaService;

    @BeforeEach
    void setUp() {
        contaService = new ContaService(contaDAO);
    }

    @Test
    void shouldRegisterValidAccount() throws SQLException {
        when(contaDAO.nomeExiste(1L, "Carteira")).thenReturn(false);
        when(contaDAO.inserir(any(Conta.class))).thenAnswer(invocation -> {
            Conta conta = invocation.getArgument(0);
            conta.setId(20L);
            return 20L;
        });

        Conta conta = contaService.cadastrar(
                1L,
                "  Carteira  ",
                TipoConta.CARTEIRA,
                "Dinheiro",
                new BigDecimal("0")
        );

        assertEquals(20L, conta.getId());
        assertEquals(new BigDecimal("0.00"), conta.getSaldoInicial());
    }

    @Test
    void shouldRejectNegativeInitialBalance() {
        assertThrows(ValidacaoException.class,
                () -> contaService.cadastrar(
                        1L,
                        "Carteira",
                        TipoConta.CARTEIRA,
                        null,
                        new BigDecimal("-1.00")
                ));
    }

    @Test
    void shouldRejectDuplicateAccountName() throws SQLException {
        when(contaDAO.nomeExiste(1L, "Carteira")).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> contaService.cadastrar(1L, "Carteira", TipoConta.CARTEIRA, null, BigDecimal.ZERO));
    }

    @Test
    void shouldReturnZeroBalanceForExistingAccount() throws SQLException {
        when(contaDAO.calcularSaldoAtual(20L, 1L)).thenReturn(Optional.of(BigDecimal.ZERO));

        BigDecimal saldo = contaService.consultarSaldoAtual(20L, 1L);

        assertEquals(BigDecimal.ZERO, saldo);
    }

    @Test
    void shouldDifferentiateZeroBalanceFromMissingAccount() throws SQLException {
        when(contaDAO.calcularSaldoAtual(21L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> contaService.consultarSaldoAtual(21L, 1L));
    }

    @Test
    void shouldRejectAccountFromAnotherUser() throws SQLException {
        when(contaDAO.buscarPorId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> contaService.buscarPorId(99L, 1L));
    }
}
