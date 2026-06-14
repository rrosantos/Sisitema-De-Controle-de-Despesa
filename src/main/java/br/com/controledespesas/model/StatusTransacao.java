package br.com.controledespesas.model;

public enum StatusTransacao {
    PENDENTE("pendente"),
    PAGO("pago"),
    RECEBIDO("recebido"),
    CANCELADO("cancelado");

    private final String valorBanco;

    StatusTransacao(String valorBanco) {
        this.valorBanco = valorBanco;
    }

    public String getValorBanco() {
        return valorBanco;
    }

    public static StatusTransacao fromValorBanco(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Database value for StatusTransacao must not be null.");
        }

        for (StatusTransacao statusTransacao : values()) {
            if (statusTransacao.valorBanco.equalsIgnoreCase(valor.trim())) {
                return statusTransacao;
            }
        }

        throw new IllegalArgumentException("Invalid database value for StatusTransacao: " + valor);
    }
}
