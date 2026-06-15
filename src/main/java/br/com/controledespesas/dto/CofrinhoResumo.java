package br.com.controledespesas.dto;

import br.com.controledespesas.model.Cofrinho;

import java.math.BigDecimal;
import java.util.Objects;

public record CofrinhoResumo(
        Cofrinho cofrinho,
        BigDecimal valorAtual,
        BigDecimal percentualProgresso
) {

    public CofrinhoResumo {
        cofrinho = Objects.requireNonNull(cofrinho, "cofrinho nao pode ser nulo.");
        valorAtual = valorAtual != null ? valorAtual : BigDecimal.ZERO;
        percentualProgresso = percentualProgresso != null ? percentualProgresso : BigDecimal.ZERO;
    }

    public boolean atingiuMeta() {
        return valorAtual.compareTo(cofrinho.getValorMeta()) >= 0;
    }
}
