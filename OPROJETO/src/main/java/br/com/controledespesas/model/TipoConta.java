package br.com.controledespesas.model;

/**
 * Enumera os tipos validos de Conta usados pelo sistema.
 */
public enum TipoConta {
    CARTEIRA("carteira"),
    CONTA_CORRENTE("conta_corrente"),
    POUPANCA("poupanca"),
    CONTA_DIGITAL("conta_digital"),
    OUTRO("outro");

    private final String valorBanco;

    TipoConta(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static TipoConta fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for TipoConta must not be null.");
        }

        for (TipoConta tipoConta : values()) {
            if (tipoConta.valorBanco.equalsIgnoreCase(valor.trim())) {
                return tipoConta;
            }
        }

        throw new IllegalArgumentException("Invalid database value for TipoConta: " + valor);
    }
}
