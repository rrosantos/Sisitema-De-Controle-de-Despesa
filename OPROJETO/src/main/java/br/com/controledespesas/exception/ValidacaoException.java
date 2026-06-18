package br.com.controledespesas.exception;

/**
 * Representa falhas de validacao tratadas pelo fluxo da aplicacao.
 */
public class ValidacaoException extends IllegalArgumentException {

    public ValidacaoException(String message) {
        super(message);
    }

    public ValidacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
