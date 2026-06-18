package br.com.controledespesas.model;

/**
 * Enumera os tipos validos de Categoria usados pelo sistema.
 */
public enum TipoCategoria {
    RECEITA("receita"),
    DESPESA("despesa");

    private final String valorBanco;

    TipoCategoria(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static TipoCategoria fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for TipoCategoria must not be null.");
        }

        for (TipoCategoria tipoCategoria : values()) {
            if (tipoCategoria.valorBanco.equalsIgnoreCase(valor.trim())) {
                return tipoCategoria;
            }
        }

        throw new IllegalArgumentException("Invalid database value for TipoCategoria: " + valor);
    }
}
