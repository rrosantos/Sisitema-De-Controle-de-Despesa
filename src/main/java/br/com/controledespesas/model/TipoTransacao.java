package br.com.controledespesas.model;

public enum TipoTransacao {
    RECEITA("receita"),
    DESPESA("despesa");

    private final String valorBanco;

    TipoTransacao(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static TipoTransacao fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for TipoTransacao must not be null.");
        }

        for (TipoTransacao tipoTransacao : values()) {
            if (tipoTransacao.valorBanco.equalsIgnoreCase(valor.trim())) {
                return tipoTransacao;
            }
        }

        throw new IllegalArgumentException("Invalid database value for TipoTransacao: " + valor);
    }
}
