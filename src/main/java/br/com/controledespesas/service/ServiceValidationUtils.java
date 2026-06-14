package br.com.controledespesas.service;

import br.com.controledespesas.exception.ValidacaoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

final class ServiceValidationUtils {

    private ServiceValidationUtils() {
    }

    static Long requireId(Long valor, String nomeCampo) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }
        if (valor <= 0) {
            throw new ValidacaoException(nomeCampo + " deve ser maior que zero.");
        }
        return valor;
    }

    static <T> T requireValue(T valor, String nomeCampo) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }
        return valor;
    }

    static String normalizeRequiredText(String valor, String nomeCampo, int tamanhoMaximo) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }

        String normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }

        if (normalizado.length() > tamanhoMaximo) {
            throw new ValidacaoException(nomeCampo + " deve ter no maximo " + tamanhoMaximo + " caracteres.");
        }

        return normalizado;
    }

    static String normalizeOptionalText(String valor, String nomeCampo, int tamanhoMaximo) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            return null;
        }

        if (normalizado.length() > tamanhoMaximo) {
            throw new ValidacaoException(nomeCampo + " deve ter no maximo " + tamanhoMaximo + " caracteres.");
        }

        return normalizado;
    }

    static String normalizeEmail(String email) {
        String normalizado = normalizeRequiredText(email, "E-mail", 255).toLowerCase(Locale.ROOT);
        if (!emailValido(normalizado)) {
            throw new ValidacaoException("E-mail invalido.");
        }
        return normalizado;
    }

    static BigDecimal normalizeMonetaryValue(BigDecimal valor, String nomeCampo, boolean permitirZero) {
        if (valor == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatorio.");
        }

        BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
        int comparacao = normalizado.compareTo(BigDecimal.ZERO);
        if (permitirZero) {
            if (comparacao < 0) {
                throw new ValidacaoException(nomeCampo + " nao pode ser negativo.");
            }
        } else if (comparacao <= 0) {
            throw new ValidacaoException(nomeCampo + " deve ser maior que zero.");
        }

        return normalizado;
    }

    static LocalDate requireDate(LocalDate data, String nomeCampo) {
        if (data == null) {
            throw new ValidacaoException(nomeCampo + " e obrigatoria.");
        }
        return data;
    }

    static void validateDateRange(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new ValidacaoException("A data inicial nao pode ser posterior a data final.");
        }
    }

    static void validatePassword(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new ValidacaoException("A senha e obrigatoria.");
        }
        if (senha.length() < 8) {
            throw new ValidacaoException("A senha deve ter pelo menos 8 caracteres.");
        }
        if (senha.length() > 72) {
            throw new ValidacaoException("A senha deve ter no maximo 72 caracteres.");
        }
    }

    static void validatePasswordConfirmation(String senha, String confirmacao) {
        if (confirmacao == null || confirmacao.isBlank()) {
            throw new ValidacaoException("A confirmacao de senha e obrigatoria.");
        }
        if (!senha.equals(confirmacao)) {
            throw new ValidacaoException("A senha e a confirmacao de senha devem ser iguais.");
        }
    }

    private static boolean emailValido(String email) {
        int indiceArroba = email.indexOf('@');
        int ultimoPonto = email.lastIndexOf('.');
        return indiceArroba > 0
                && ultimoPonto > indiceArroba + 1
                && ultimoPonto < email.length() - 1
                && email.indexOf(' ') < 0;
    }
}
