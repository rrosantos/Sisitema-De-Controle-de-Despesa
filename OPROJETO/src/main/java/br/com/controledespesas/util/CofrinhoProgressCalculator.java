package br.com.controledespesas.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Calcula o percentual de progresso de metas de cofrinho com BigDecimal.
 */
public final class CofrinhoProgressCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private CofrinhoProgressCalculator() {
    }

    public static BigDecimal calculateProgressPercentage(BigDecimal currentValue, BigDecimal targetValue) {
        Objects.requireNonNull(currentValue, "Current value must not be null.");
        Objects.requireNonNull(targetValue, "Target value must not be null.");

        if (targetValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target value must be greater than zero.");
        }

        return currentValue
                .multiply(ONE_HUNDRED)
                .divide(targetValue, 2, RoundingMode.HALF_UP);
    }
}
