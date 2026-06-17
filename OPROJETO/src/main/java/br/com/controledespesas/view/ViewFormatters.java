package br.com.controledespesas.view;

import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.StatusTransacao;

final class ViewFormatters {

    private static final String DASH = "-";

    private ViewFormatters() {
    }

    static String formatTipoCategoria(TipoCategoria tipoCategoria) {
        if (tipoCategoria == null) {
            return DASH;
        }

        return switch (tipoCategoria) {
            case RECEITA -> "Receita";
            case DESPESA -> "Despesa";
        };
    }

    static String formatTipoConta(TipoConta tipoConta) {
        if (tipoConta == null) {
            return DASH;
        }

        return switch (tipoConta) {
            case CARTEIRA -> "Carteira";
            case CONTA_CORRENTE -> "Conta-corrente";
            case POUPANCA -> "Poupanca";
            case CONTA_DIGITAL -> "Conta digital";
            case OUTRO -> "Outro";
        };
    }

    static String formatTipoTransacao(TipoTransacao tipoTransacao) {
        if (tipoTransacao == null) {
            return DASH;
        }

        return switch (tipoTransacao) {
            case RECEITA -> "Receita";
            case DESPESA -> "Despesa";
        };
    }

    static String formatStatusTransacao(StatusTransacao statusTransacao) {
        if (statusTransacao == null) {
            return DASH;
        }

        return switch (statusTransacao) {
            case PENDENTE -> "Pendente";
            case PAGO -> "Pago";
            case RECEBIDO -> "Recebido";
            case CANCELADO -> "Cancelado";
        };
    }

    static String formatStatusCofrinho(StatusCofrinho statusCofrinho) {
        if (statusCofrinho == null) {
            return DASH;
        }

        return switch (statusCofrinho) {
            case EM_ANDAMENTO -> "Em andamento";
            case CONCLUIDO -> "Concluido";
            case CANCELADO -> "Cancelado";
        };
    }

    static String formatTipoMovimentacaoCofrinho(TipoMovimentacaoCofrinho tipoMovimentacao) {
        if (tipoMovimentacao == null) {
            return DASH;
        }

        return switch (tipoMovimentacao) {
            case DEPOSITO -> "Deposito";
            case RETIRADA -> "Retirada";
        };
    }

    static String formatStatus(boolean ativo) {
        return ativo ? "Ativa" : "Inativa";
    }

    static String formatOptionalText(String texto) {
        return texto == null || texto.isBlank() ? DASH : texto;
    }
}
