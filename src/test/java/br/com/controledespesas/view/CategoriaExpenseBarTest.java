package br.com.controledespesas.view;

import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoriaExpenseBarTest {

    @Test
    void shouldCalculateVisualPercentageWithinExpectedLimits() {
        assertEquals(0, CategoriaExpenseBar.calcularPercentualVisual(BigDecimal.ZERO));
        assertEquals(42, CategoriaExpenseBar.calcularPercentualVisual(new BigDecimal("41.60")));
        assertEquals(100, CategoriaExpenseBar.calcularPercentualVisual(new BigDecimal("100.00")));
        assertEquals(100, CategoriaExpenseBar.calcularPercentualVisual(new BigDecimal("180.30")));
    }

    @Test
    void shouldExposeFormattedValueAndPercentageTexts() {
        CategoriaExpenseBar bar = new CategoriaExpenseBar(new ResumoCategoriaDashboard(
                1L,
                "Moradia",
                new BigDecimal("750.00"),
                new BigDecimal("50.00")
        ));

        assertEquals(new MoneyFormatter().format(new BigDecimal("750.00")), bar.obterTextoValor());
        assertEquals("50,00%", bar.obterTextoPercentual());
    }
}
