package br.com.controledespesas.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CofrinhoProgressCalculatorTest {

    @Test
    void shouldCalculateProgressPercentageWithTwoDecimalPlaces() {
        BigDecimal percentual = CofrinhoProgressCalculator.calculateProgressPercentage(
                new BigDecimal("250.00"),
                new BigDecimal("1000.00")
        );

        assertEquals(new BigDecimal("25.00"), percentual);
    }

    @Test
    void shouldAllowProgressAboveOneHundredPercent() {
        BigDecimal percentual = CofrinhoProgressCalculator.calculateProgressPercentage(
                new BigDecimal("1250.00"),
                new BigDecimal("1000.00")
        );

        assertEquals(new BigDecimal("125.00"), percentual);
    }

    @Test
    void shouldRejectNullOrNonPositiveTargetValues() {
        assertThrows(IllegalArgumentException.class,
                () -> CofrinhoProgressCalculator.calculateProgressPercentage(new BigDecimal("10.00"), BigDecimal.ZERO));

        assertThrows(NullPointerException.class,
                () -> CofrinhoProgressCalculator.calculateProgressPercentage(new BigDecimal("10.00"), null));
    }
}
