package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoTransacao;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class TransacaoFormSupport {

    private TransacaoFormSupport() {
    }

    static List<StatusTransacao> statusDisponiveis(TipoTransacao tipoTransacao) {
        if (tipoTransacao == null) {
            return List.of();
        }

        return switch (tipoTransacao) {
            case RECEITA -> List.of(StatusTransacao.PENDENTE, StatusTransacao.RECEBIDO, StatusTransacao.CANCELADO);
            case DESPESA -> List.of(StatusTransacao.PENDENTE, StatusTransacao.PAGO, StatusTransacao.CANCELADO);
        };
    }

    static List<Categoria> categoriasDisponiveis(List<Categoria> categorias, TipoTransacao tipoTransacao,
                                                 Long categoriaHistoricaId) {
        if (categorias == null || tipoTransacao == null) {
            return List.of();
        }

        TipoCategoria tipoCategoria = switch (tipoTransacao) {
            case RECEITA -> TipoCategoria.RECEITA;
            case DESPESA -> TipoCategoria.DESPESA;
        };

        return categorias.stream()
                .filter(categoria -> categoria.getTipo() == tipoCategoria)
                .filter(categoria -> categoria.isAtivo() || Objects.equals(categoria.getId(), categoriaHistoricaId))
                .sorted(Comparator.comparing(Categoria::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static List<Conta> contasDisponiveis(List<Conta> contas, Long contaHistoricaId) {
        if (contas == null) {
            return List.of();
        }

        return contas.stream()
                .filter(conta -> conta.isAtivo() || Objects.equals(conta.getId(), contaHistoricaId))
                .sorted(Comparator.comparing(Conta::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
