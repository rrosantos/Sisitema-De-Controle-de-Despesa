package br.com.controledespesas.view;

import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;

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

    static String formatStatus(boolean ativo) {
        return ativo ? "Ativa" : "Inativa";
    }

    static String formatOptionalText(String texto) {
        return texto == null || texto.isBlank() ? DASH : texto;
    }
}
