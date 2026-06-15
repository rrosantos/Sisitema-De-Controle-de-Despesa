package br.com.controledespesas.view;

import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CofrinhoViewSupportTest {

    @Test
    void shouldDetectOverdueGoalOnlyWhenStillInProgress() {
        Cofrinho emAtraso = cofrinho(StatusCofrinho.EM_ANDAMENTO, LocalDate.of(2026, 6, 10));
        Cofrinho concluido = cofrinho(StatusCofrinho.CONCLUIDO, LocalDate.of(2026, 6, 10));

        assertTrue(CofrinhoViewSupport.estaAtrasado(emAtraso, LocalDate.of(2026, 6, 15)));
        assertFalse(CofrinhoViewSupport.estaAtrasado(concluido, LocalDate.of(2026, 6, 15)));
    }

    @Test
    void shouldClampVisualPercentageWithoutChangingRealPercentageFormatting() {
        assertEquals("135,55%", CofrinhoViewSupport.formatarPercentual(new BigDecimal("135.55")));
        assertEquals(100, CofrinhoViewSupport.percentualVisual(new BigDecimal("135.55")));
        assertEquals(0, CofrinhoViewSupport.percentualVisual(new BigDecimal("-5.00")));
    }

    @Test
    void shouldFormatStatusAndDeadlineLabels() {
        assertEquals("Em andamento", CofrinhoViewSupport.formatarStatus(StatusCofrinho.EM_ANDAMENTO));
        assertEquals("Concluido", CofrinhoViewSupport.formatarStatus(StatusCofrinho.CONCLUIDO));
        assertEquals("Cancelado", CofrinhoViewSupport.formatarStatus(StatusCofrinho.CANCELADO));
        assertEquals("Sem prazo", CofrinhoViewSupport.formatarPrazo(null));
    }

    private Cofrinho cofrinho(StatusCofrinho status, LocalDate dataLimite) {
        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setStatus(status);
        cofrinho.setDataLimite(dataLimite);
        return cofrinho;
    }
}
