package br.com.controledespesas.service;

import br.com.controledespesas.dao.DashboardDAO;
import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.dto.ResumoContaDashboard;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardDAO dashboardDAO;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardDAO);
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(ValidacaoException.class, () -> dashboardService.carregar(null, null, null));
    }

    @Test
    void shouldRejectInvalidDateRange() {
        assertThrows(
                ValidacaoException.class,
                () -> dashboardService.carregar(1L, LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 10))
        );
    }

    @Test
    void shouldNormalizeNullValuesAndLists() throws Exception {
        when(dashboardDAO.listarSaldosPorConta(1L)).thenReturn(null);
        when(dashboardDAO.calcularReceitasPeriodo(1L, null, null)).thenReturn(null);
        when(dashboardDAO.calcularDespesasPeriodo(1L, null, null)).thenReturn(null);
        when(dashboardDAO.listarDespesasPorCategoria(1L, null, null, 5)).thenReturn(null);
        when(dashboardDAO.listarTransacoesRecentes(1L, null, null, 5)).thenReturn(null);
        when(dashboardDAO.listarResumoCofrinhos(1L, 4)).thenReturn(null);
        when(dashboardDAO.contarTransacoesPendentes(1L)).thenReturn(0);

        DashboardResumo resumo = dashboardService.carregar(1L, null, null);

        assertEquals(0, resumo.saldoTotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumo.totalReceitas().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumo.totalDespesas().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumo.resultadoPeriodo().compareTo(BigDecimal.ZERO));
        assertEquals(0, resumo.contasAtivas());
        assertEquals(0, resumo.transacoesPendentes());
        assertTrue(resumo.contas().isEmpty());
        assertTrue(resumo.despesasPorCategoria().isEmpty());
        assertTrue(resumo.transacoesRecentes().isEmpty());
        assertTrue(resumo.cofrinhos().isEmpty());
    }

    @Test
    void shouldAssembleSummaryWithPositiveResultAndCategoryPercentages() throws Exception {
        when(dashboardDAO.listarSaldosPorConta(1L)).thenReturn(List.of(
                new ResumoContaDashboard(10L, "Carteira", TipoConta.CARTEIRA, true, new BigDecimal("100.00")),
                new ResumoContaDashboard(11L, "Reserva", TipoConta.POUPANCA, false, new BigDecimal("250.00"))
        ));
        when(dashboardDAO.calcularReceitasPeriodo(1L, null, null)).thenReturn(new BigDecimal("500.00"));
        when(dashboardDAO.calcularDespesasPeriodo(1L, null, null)).thenReturn(new BigDecimal("200.00"));
        when(dashboardDAO.listarDespesasPorCategoria(1L, null, null, 5)).thenReturn(List.of(
                new ResumoCategoriaDashboard(1L, "Moradia", new BigDecimal("120.00"), null),
                new ResumoCategoriaDashboard(2L, "Lazer", new BigDecimal("80.00"), null)
        ));
        when(dashboardDAO.listarTransacoesRecentes(1L, null, null, 5)).thenReturn(List.of());
        when(dashboardDAO.listarResumoCofrinhos(1L, 4)).thenReturn(List.of(
                new ResumoCofrinhoDashboard(
                        30L,
                        "Viagem",
                        StatusCofrinho.CONCLUIDO,
                        new BigDecimal("450.00"),
                        new BigDecimal("300.00"),
                        null,
                        LocalDate.of(2026, 7, 10)
                )
        ));
        when(dashboardDAO.contarTransacoesPendentes(1L)).thenReturn(2);

        DashboardResumo resumo = dashboardService.carregar(1L, null, null);

        assertEquals(0, resumo.saldoTotal().compareTo(new BigDecimal("350.00")));
        assertEquals(1, resumo.contasAtivas());
        assertEquals(0, resumo.resultadoPeriodo().compareTo(new BigDecimal("300.00")));
        assertEquals(0, resumo.despesasPorCategoria().get(0).percentual().compareTo(new BigDecimal("60.00")));
        assertEquals(0, resumo.despesasPorCategoria().get(1).percentual().compareTo(new BigDecimal("40.00")));
        assertEquals(0, resumo.cofrinhos().get(0).percentual().compareTo(new BigDecimal("150.00")));
        assertEquals(2, resumo.transacoesPendentes());
    }

    @Test
    void shouldCalculateNegativeAndZeroResults() throws Exception {
        when(dashboardDAO.listarSaldosPorConta(1L)).thenReturn(List.of());
        when(dashboardDAO.listarDespesasPorCategoria(1L, null, null, 5)).thenReturn(List.of());
        when(dashboardDAO.listarTransacoesRecentes(1L, null, null, 5)).thenReturn(List.of());
        when(dashboardDAO.listarResumoCofrinhos(1L, 4)).thenReturn(List.of());
        when(dashboardDAO.contarTransacoesPendentes(1L)).thenReturn(0);

        when(dashboardDAO.calcularReceitasPeriodo(1L, null, null)).thenReturn(new BigDecimal("100.00"));
        when(dashboardDAO.calcularDespesasPeriodo(1L, null, null)).thenReturn(new BigDecimal("250.00"));
        DashboardResumo negativo = dashboardService.carregar(1L, null, null);

        when(dashboardDAO.calcularReceitasPeriodo(1L, null, null)).thenReturn(new BigDecimal("200.00"));
        when(dashboardDAO.calcularDespesasPeriodo(1L, null, null)).thenReturn(new BigDecimal("200.00"));
        DashboardResumo zerado = dashboardService.carregar(1L, null, null);

        assertEquals(0, negativo.resultadoPeriodo().compareTo(new BigDecimal("-150.00")));
        assertEquals(0, zerado.resultadoPeriodo().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldKeepCategoryPercentagesAtZeroWhenThereAreNoExpenses() throws Exception {
        when(dashboardDAO.listarSaldosPorConta(1L)).thenReturn(List.of());
        when(dashboardDAO.calcularReceitasPeriodo(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dashboardDAO.calcularDespesasPeriodo(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dashboardDAO.listarDespesasPorCategoria(eq(1L), any(), any(), any(Integer.class))).thenReturn(List.of(
                new ResumoCategoriaDashboard(1L, "Teste", BigDecimal.ZERO, null)
        ));
        when(dashboardDAO.listarTransacoesRecentes(eq(1L), any(), any(), any(Integer.class))).thenReturn(List.of());
        when(dashboardDAO.listarResumoCofrinhos(1L, 4)).thenReturn(List.of());
        when(dashboardDAO.contarTransacoesPendentes(1L)).thenReturn(0);

        DashboardResumo resumo = dashboardService.carregar(
                1L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30)
        );

        assertEquals(0, resumo.despesasPorCategoria().get(0).percentual().compareTo(BigDecimal.ZERO));
    }
}
