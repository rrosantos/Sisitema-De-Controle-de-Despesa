package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;

import java.util.Locale;
import java.util.function.Function;

public enum CampoPesquisaConta {
    NOME("Nome", Conta::getNome),
    INSTITUICAO("Instituição", Conta::getInstituicao);

    private final String descricao;
    private final Function<Conta, String> extratorValor;

    CampoPesquisaConta(String descricao, Function<Conta, String> extratorValor) {
        this.descricao = descricao;
        this.extratorValor = extratorValor;
    }

    public String getDescricao() {
        return descricao;
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
        return valor != null && normalizar(valor).contains(termoNormalizado);
    }

    @Override
    public String toString() {
        return descricao;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
