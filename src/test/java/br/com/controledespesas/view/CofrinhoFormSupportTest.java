package br.com.controledespesas.view;

import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CofrinhoFormSupportTest {

    private final MoneyFormatter moneyFormatter = new MoneyFormatter();

    @Test
    void shouldCreateSavingsGoalFormData() {
        DadosCofrinhoForm dados = CofrinhoFormSupport.criarDadosCofrinho(
                "Reserva de viagem",
                "Ferias de dezembro",
                "1.250,90",
                "20/12/2026",
                moneyFormatter
        );

        assertEquals("Reserva de viagem", dados.nome());
        assertEquals("Ferias de dezembro", dados.descricao());
        assertEquals(new BigDecimal("1250.90"), dados.valorMeta());
        assertEquals(LocalDate.of(2026, 12, 20), dados.dataLimite());
    }

    @Test
    void shouldRejectBlankSavingsGoalName() {
        ValidacaoException exception = assertThrows(
                ValidacaoException.class,
                () -> CofrinhoFormSupport.criarDadosCofrinho(" ", null, "10,00", "", moneyFormatter)
        );

        assertEquals("Nome do cofrinho e obrigatorio.", exception.getMessage());
    }

    @Test
    void shouldCreateMovementFormDataAndRejectNonPositiveValues() {
        DadosMovimentacaoCofrinhoForm dados = CofrinhoFormSupport.criarDadosMovimentacao(
                "50,00",
                "15/06/2026",
                "Aporte",
                moneyFormatter
        );

        assertEquals(new BigDecimal("50.00"), dados.valor());
        assertEquals(LocalDate.of(2026, 6, 15), dados.dataMovimentacao());
        assertEquals("Aporte", dados.observacao());

        ValidacaoException exception = assertThrows(
                ValidacaoException.class,
                () -> CofrinhoFormSupport.criarDadosMovimentacao("0,00", "15/06/2026", null, moneyFormatter)
        );

        assertEquals("O valor da movimentacao deve ser maior que zero.", exception.getMessage());
    }
}
