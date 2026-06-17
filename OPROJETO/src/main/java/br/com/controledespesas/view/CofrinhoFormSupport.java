package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

final class CofrinhoFormSupport {

    private static final String MENSAGEM_VALOR_META_ZERO = "O valor da meta deve ser maior que zero.";
    private static final String MENSAGEM_VALOR_MOVIMENTACAO_ZERO = "O valor da movimentacao deve ser maior que zero.";

    private CofrinhoFormSupport() {
    }

    static DadosCofrinhoForm criarDadosCofrinho(String nome, String descricao, String valorMetaTexto,
                                                String dataLimiteTexto, MoneyFormatter moneyFormatter) {
        Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");

        String nomeNormalizado = nome != null ? nome.trim() : "";
        if (nomeNormalizado.isBlank()) {
            throw new ValidacaoException("Nome do cofrinho e obrigatorio.");
        }

        String valorTextoNormalizado = valorMetaTexto != null ? valorMetaTexto.trim() : "";
        if (valorTextoNormalizado.isBlank()) {
            throw new ValidacaoException("Valor da meta e obrigatorio.");
        }

        BigDecimal valorMeta = moneyFormatter.parse(valorTextoNormalizado);
        if (valorMeta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException(MENSAGEM_VALOR_META_ZERO);
        }

        LocalDate dataLimite = DateFormatter.parse(dataLimiteTexto);
        return new DadosCofrinhoForm(nomeNormalizado, descricao, valorMeta, dataLimite);
    }

    static DadosMovimentacaoCofrinhoForm criarDadosMovimentacao(String valorTexto, String dataTexto, String observacao,
                                                                MoneyFormatter moneyFormatter) {
        Objects.requireNonNull(moneyFormatter, "moneyFormatter nao pode ser nulo.");

        String valorTextoNormalizado = valorTexto != null ? valorTexto.trim() : "";
        if (valorTextoNormalizado.isBlank()) {
            throw new ValidacaoException("Valor da movimentacao e obrigatorio.");
        }

        String dataTextoNormalizado = dataTexto != null ? dataTexto.trim() : "";
        if (dataTextoNormalizado.isBlank()) {
            throw new ValidacaoException("Data da movimentacao e obrigatoria.");
        }

        BigDecimal valor = moneyFormatter.parse(valorTextoNormalizado);
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacaoException(MENSAGEM_VALOR_MOVIMENTACAO_ZERO);
        }

        LocalDate dataMovimentacao = DateFormatter.parse(dataTextoNormalizado);
        if (dataMovimentacao == null) {
            throw new ValidacaoException("Data da movimentacao e obrigatoria.");
        }

        return new DadosMovimentacaoCofrinhoForm(valor, dataMovimentacao, observacao);
    }
}
