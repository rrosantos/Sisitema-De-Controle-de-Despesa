package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateFormatterTest {

    @Test
    void shouldParseAndFormatBrazilianDates() {
        assertEquals(LocalDate.of(2026, 1, 1), DateFormatter.parse("01/01/2026"));
        assertEquals(LocalDate.of(2026, 12, 31), DateFormatter.parse("31/12/2026"));
        assertEquals(LocalDate.of(2026, 6, 10), DateFormatter.parse(" 10/06/2026 "));
        assertEquals("10/06/2026", DateFormatter.format(LocalDate.of(2026, 6, 10)));
    }

    @Test
    void shouldReturnNullForBlankFilterDate() {
        assertNull(DateFormatter.parse(""));
        assertNull(DateFormatter.parse("   "));
    }

    @Test
    void shouldRejectInvalidDates() {
        assertThrows(ValidacaoException.class, () -> DateFormatter.parse("31/02/2026"));
        assertThrows(ValidacaoException.class, () -> DateFormatter.parse("32/01/2026"));
        assertThrows(ValidacaoException.class, () -> DateFormatter.parse("2026-06-10"));
    }
}
