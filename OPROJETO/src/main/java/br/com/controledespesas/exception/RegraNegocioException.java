package br.com.controledespesas.exception;

/**
 * Representa falhas de regranegocio tratadas pelo fluxo da aplicacao.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }

    public RegraNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
