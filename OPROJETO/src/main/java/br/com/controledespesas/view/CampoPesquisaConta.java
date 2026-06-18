package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;

import java.util.Locale;
import java.util.function.Function;

/**
 * Enumera opcoes fixas usadas no fluxo de CampoPesquisaConta.
 */
public enum CampoPesquisaConta {
    ID("ID", true, conta -> conta.getId() != null ? String.valueOf(conta.getId()) : null),
    NOME("Nome", Conta::getNome),
    INSTITUICAO("Instituição", Conta::getInstituicao);

    private final String descricao;
    private final boolean numerico;
    private final Function<Conta, String> extratorValor;

    CampoPesquisaConta(String descricao, Function<Conta, String> extratorValor) {
        this(descricao, false, extratorValor);
    }

    CampoPesquisaConta(String descricao, boolean numerico, Function<Conta, String> extratorValor) {
        this.descricao = descricao;
        this.numerico = numerico;
        this.extratorValor = extratorValor;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isNumerico() {
        return numerico;
    }

    public boolean corresponde(Conta conta, String termo) {
        String termoNormalizado = normalizar(termo);
        if (termoNormalizado.isEmpty()) {
            return true;
        }

        if (conta == null) {
            return false;
        }

        String valor = extratorValor.apply(conta);
        if (valor == null) {
            return false;
        }

        String valorNormalizado = normalizar(valor);
        return numerico ? valorNormalizado.equals(termoNormalizado) : valorNormalizado.contains(termoNormalizado);
    }

    @Override
    public String toString() {
        return descricao;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
