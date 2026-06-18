package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Converte e formata datas no padrao dd/MM/aaaa usado pela interface.
 */
public final class DateFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.of("pt", "BR"))
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String MENSAGEM_INVALIDA = "Informe uma data valida no formato dd/MM/aaaa.";

    private DateFormatter() {
    }

    public static LocalDate parse(String texto) {
        if (texto == null) {
            return null;
        }

        String normalizado = texto.trim();
        if (normalizado.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(normalizado, FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ValidacaoException(MENSAGEM_INVALIDA, exception);
        }
    }

    public static String format(LocalDate data) {
        if (data == null) {
            return "";
        }
        return FORMATTER.format(data);
    }
}
