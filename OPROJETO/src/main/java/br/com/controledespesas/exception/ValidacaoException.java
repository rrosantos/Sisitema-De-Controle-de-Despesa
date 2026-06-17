package br.com.controledespesas.exception;

public class ValidacaoException extends IllegalArgumentException {

    public ValidacaoException(String message) {
        super(message);
    }

    public ValidacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
