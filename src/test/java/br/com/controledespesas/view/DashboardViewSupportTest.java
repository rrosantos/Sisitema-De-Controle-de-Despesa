package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardViewSupportTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldFormatCurrencyAndSignedResults() {
        assertEquals(moneyFormatter.format(new BigDecimal("1234.56")),
                DashboardViewSupport.formatarMoeda(new BigDecimal("1234.56")));
        assertEquals("+" + moneyFormatter.format(new BigDecimal("80.00")),
                DashboardViewSupport.formatarResultado(new BigDecimal("80.00")));
        assertEquals(moneyFormatter.format(new BigDecimal("-25.00")),
                DashboardViewSupport.formatarResultado(new BigDecimal("-25.00")));
        assertEquals(moneyFormatter.format(BigDecimal.ZERO.setScale(2)),
                DashboardViewSupport.formatarResultado(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void shouldLimitVisualPercentageAndFormatTexts() {
        assertEquals(0, DashboardViewSupport.percentualVisual(BigDecimal.ZERO));
        assertEquals(38, DashboardViewSupport.percentualVisual(new BigDecimal("37.50")));
        assertEquals(100, DashboardViewSupport.percentualVisual(new BigDecimal("100.00")));
        assertEquals(100, DashboardViewSupport.percentualVisual(new BigDecimal("155.32")));
        assertEquals("Ativa", DashboardViewSupport.formatarStatusConta(true));
        assertEquals("Inativa", DashboardViewSupport.formatarStatusConta(false));
        assertEquals("Conta-corrente", DashboardViewSupport.formatarTipoConta(TipoConta.CONTA_CORRENTE));
        assertEquals("Concluido", DashboardViewSupport.formatarStatusCofrinho(StatusCofrinho.CONCLUIDO));
    }

    @Test
    void shouldIdentifyLateGoals() {
        ResumoCofrinhoDashboard atrasado = new ResumoCofrinhoDashboard(
                1L,
                "Reserva",
                StatusCofrinho.EM_ANDAMENTO,
                new BigDecimal("300.00"),
                new BigDecimal("500.00"),
                new BigDecimal("60.00"),
                LocalDate.of(2026, 6, 10)
        );
        ResumoCofrinhoDashboard concluido = new ResumoCofrinhoDashboard(
                2L,
                "Viagem",
                StatusCofrinho.CONCLUIDO,
                new BigDecimal("900.00"),
                new BigDecimal("800.00"),
                new BigDecimal("112.50"),
                LocalDate.of(2026, 6, 10)
        );

        assertTrue(DashboardViewSupport.metaAtrasada(atrasado, LocalDate.of(2026, 6, 15)));
        assertFalse(DashboardViewSupport.metaAtrasada(concluido, LocalDate.of(2026, 6, 15)));
        assertEquals("10/06/2026", DashboardViewSupport.formatarPrazo(LocalDate.of(2026, 6, 10)));
        assertEquals("Sem prazo", DashboardViewSupport.formatarPrazo(null));
    }
}
