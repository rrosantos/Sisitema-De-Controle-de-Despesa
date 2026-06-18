package br.com.controledespesas.exception;

/**
 * Representa falhas de autenticacao tratadas pelo fluxo da aplicacao.
 */
public class AutenticacaoException extends RuntimeException {

    public AutenticacaoException(String message) {
        super(message);
    }

    public AutenticacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
