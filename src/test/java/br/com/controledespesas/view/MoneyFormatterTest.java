package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyFormatterTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldParseBrazilianInputsToBigDecimal() {
        assertEquals(new BigDecimal("0.00"), moneyFormatter.parse("0"));
        assertEquals(new BigDecimal("10.00"), moneyFormatter.parse("10"));
        assertEquals(new BigDecimal("10.50"), moneyFormatter.parse("10,50"));
        assertEquals(new BigDecimal("1250.75"), moneyFormatter.parse("1.250,75"));
        assertEquals(new BigDecimal("25.90"), moneyFormatter.parse(" 25,90 "));
    }

    @Test
    void shouldRejectInvalidOrNegativeValues() {
        assertThrows(ValidacaoException.class, () -> moneyFormatter.parse("abc"));
        assertThrows(ValidacaoException.class, () -> moneyFormatter.parse("-5,00"));
    }

    @Test
    void shouldFormatValuesForDisplayAndInput() {
        String valorFormatado = moneyFormatter.format(new BigDecimal("1250.75"));

        assertTrue(valorFormatado.startsWith("R$"));
        assertTrue(valorFormatado.contains("1.250,75"));
        assertEquals("1.250,75", moneyFormatter.formatForInput(new BigDecimal("1250.75")));
    }
}
