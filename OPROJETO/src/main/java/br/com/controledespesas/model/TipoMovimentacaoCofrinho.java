package br.com.controledespesas.model;

/**
 * Enumera os tipos validos de MovimentacaoCofrinho usados pelo sistema.
 */
public enum TipoMovimentacaoCofrinho {
    DEPOSITO("deposito"),
    RETIRADA("retirada");

    private final String valorBanco;

    TipoMovimentacaoCofrinho(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static TipoMovimentacaoCofrinho fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for TipoMovimentacaoCofrinho must not be null.");
        }

        for (TipoMovimentacaoCofrinho tipoMovimentacao : values()) {
            if (tipoMovimentacao.valorBanco.equalsIgnoreCase(valor.trim())) {
                return tipoMovimentacao;
            }
        }

        throw new IllegalArgumentException("Invalid database value for TipoMovimentacaoCofrinho: " + valor);
    }
}
