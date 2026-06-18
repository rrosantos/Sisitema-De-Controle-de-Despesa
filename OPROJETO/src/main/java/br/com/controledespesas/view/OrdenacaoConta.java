package br.com.controledespesas.view;

/**
 * Enumera opcoes fixas usadas no fluxo de OrdenacaoConta.
 */
public enum OrdenacaoConta {
    NOME_CRESCENTE("Nome: A-Z"),
    NOME_DECRESCENTE("Nome: Z-A"),
    MAIOR_SALDO("Maior saldo atual"),
    MENOR_SALDO("Menor saldo atual");

    private final String descricao;

    OrdenacaoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
