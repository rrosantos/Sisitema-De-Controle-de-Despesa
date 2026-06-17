package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ContaListSupport {

    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");

    public List<Conta> filtrarEOrdenar(
            List<Conta> contas,
            Map<Long, BigDecimal> saldos,
            CampoPesquisaConta campoPesquisa,
            String termo,
            TipoConta tipo,
            Boolean ativo,
            OrdenacaoConta ordenacao
    ) {
        CampoPesquisaConta campoSelecionado = campoPesquisa != null ? campoPesquisa : CampoPesquisaConta.NOME;
        OrdenacaoConta ordenacaoSelecionada = ordenacao != null ? ordenacao : OrdenacaoConta.NOME_CRESCENTE;

        List<Conta> resultado = new ArrayList<>();
        if (contas != null) {
            contas.stream()
                    .filter(conta -> filtrarPorTipo(conta, tipo))
                    .filter(conta -> filtrarPorStatus(conta, ativo))
                    .filter(conta -> campoSelecionado.corresponde(conta, termo))
                    .forEach(resultado::add);
        }

        resultado.sort(comparador(ordenacaoSelecionada, saldos));
        return resultado;
    }

    private boolean filtrarPorTipo(Conta conta, TipoConta tipo) {
        return tipo == null || (conta != null && conta.getTipo() == tipo);
    }

    private boolean filtrarPorStatus(Conta conta, Boolean ativo) {
        return ativo == null || (conta != null && conta.isAtivo() == ativo);
    }

    private Comparator<Conta> comparador(OrdenacaoConta ordenacao, Map<Long, BigDecimal> saldos) {
        return switch (ordenacao) {
            case NOME_CRESCENTE -> compararPorNome(true);
            case NOME_DECRESCENTE -> compararPorNome(false);
            case MAIOR_SALDO -> compararPorSaldo(saldos, false);
            case MENOR_SALDO -> compararPorSaldo(saldos, true);
        };
    }

    private Comparator<Conta> compararPorNome(boolean crescente) {
        Comparator<Conta> comparadorNome = (primeira, segunda) -> compararTexto(
                valorTexto(primeira != null ? primeira.getNome() : null),
                valorTexto(segunda != null ? segunda.getNome() : null)
        );

        if (!crescente) {
            comparadorNome = comparadorNome.reversed();
        }

        return comparadorNome
                .thenComparing((primeira, segunda) -> compararTexto(
                        valorTexto(primeira != null ? primeira.getInstituicao() : null),
                        valorTexto(segunda != null ? segunda.getInstituicao() : null)
                ))
                .thenComparing(this::compararId);
    }

    private Comparator<Conta> compararPorSaldo(Map<Long, BigDecimal> saldos, boolean crescente) {
        return (primeira, segunda) -> {
            BigDecimal saldoPrimeira = obterSaldo(saldos, primeira);
            BigDecimal saldoSegunda = obterSaldo(saldos, segunda);
            boolean primeiraPossuiSaldo = saldoPrimeira != null;
            boolean segundaPossuiSaldo = saldoSegunda != null;

            if (primeiraPossuiSaldo && !segundaPossuiSaldo) {
                return -1;
            }
            if (!primeiraPossuiSaldo && segundaPossuiSaldo) {
                return 1;
            }
            if (primeiraPossuiSaldo) {
                int comparacaoSaldo = saldoPrimeira.compareTo(saldoSegunda);
                if (comparacaoSaldo != 0) {
                    return crescente ? comparacaoSaldo : -comparacaoSaldo;
                }
            }

            int comparacaoNome = compararTexto(
                    valorTexto(primeira != null ? primeira.getNome() : null),
                    valorTexto(segunda != null ? segunda.getNome() : null)
            );
            if (comparacaoNome != 0) {
                return comparacaoNome;
            }
            return compararId(primeira, segunda);
        };
    }

    private BigDecimal obterSaldo(Map<Long, BigDecimal> saldos, Conta conta) {
        if (saldos == null || conta == null || conta.getId() == null) {
            return null;
        }
        return saldos.get(conta.getId());
    }

    private int compararTexto(String primeiro, String segundo) {
        return criarCollator().compare(primeiro, segundo);
    }

    private Collator criarCollator() {
        Collator collator = Collator.getInstance(LOCALE_PT_BR);
        collator.setStrength(Collator.PRIMARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return collator;
    }

    private String valorTexto(String valor) {
        return valor != null ? valor.trim() : "";
    }

    private int compararId(Conta primeira, Conta segunda) {
        Long idPrimeira = primeira != null ? primeira.getId() : null;
        Long idSegunda = segunda != null ? segunda.getId() : null;
        if (idPrimeira == null && idSegunda == null) {
            return 0;
        }
        if (idPrimeira == null) {
            return 1;
        }
        if (idSegunda == null) {
            return -1;
        }
        return idPrimeira.compareTo(idSegunda);
    }
}
