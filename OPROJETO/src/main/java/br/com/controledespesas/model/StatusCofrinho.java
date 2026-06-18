package br.com.controledespesas.model;

/**
 * Enumera os status validos de Cofrinho usados pelo sistema.
 */
public enum StatusCofrinho {
    EM_ANDAMENTO("em_andamento"),
    CONCLUIDO("concluido"),
    CANCELADO("cancelado");

    private final String valorBanco;

    StatusCofrinho(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static StatusCofrinho fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for StatusCofrinho must not be null.");
        }

        for (StatusCofrinho statusCofrinho : values()) {
            if (statusCofrinho.valorBanco.equalsIgnoreCase(valor.trim())) {
                return statusCofrinho;
            }
        }

        throw new IllegalArgumentException("Invalid database value for StatusCofrinho: " + valor);
    }
}
