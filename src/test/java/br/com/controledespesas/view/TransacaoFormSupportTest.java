package br.com.controledespesas.view;

import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransacaoFormSupportTest {

    @Test
    void shouldReturnValidStatusesForEachTransactionType() {
        assertEquals(
                List.of(StatusTransacao.PENDENTE, StatusTransacao.RECEBIDO, StatusTransacao.CANCELADO),
                TransacaoFormSupport.statusDisponiveis(TipoTransacao.RECEITA)
        );
        assertEquals(
                List.of(StatusTransacao.PENDENTE, StatusTransacao.PAGO, StatusTransacao.CANCELADO),
                TransacaoFormSupport.statusDisponiveis(TipoTransacao.DESPESA)
        );
    }

    @Test
    void shouldFilterCategoriesByTypeAndKeepHistoricalInactiveCategory() {
        Categoria salario = categoria(1L, "Salario", TipoCategoria.RECEITA, true);
        Categoria bonus = categoria(2L, "Bonus antigo", TipoCategoria.RECEITA, false);
        Categoria mercado = categoria(3L, "Mercado", TipoCategoria.DESPESA, true);

        List<Categoria> resultado = TransacaoFormSupport.categoriasDisponiveis(
                List.of(salario, bonus, mercado),
                TipoTransacao.RECEITA,
                2L
        );

        assertEquals(List.of(bonus, salario), resultado);
    }

    @Test
    void shouldKeepOnlyActiveAccountsOrHistoricalInactiveAccount() {
        Conta carteira = conta(1L, "Carteira", true);
        Conta bancoAntigo = conta(2L, "Banco antigo", false);
        Conta reserva = conta(3L, "Reserva", true);

        List<Conta> resultado = TransacaoFormSupport.contasDisponiveis(List.of(carteira, bancoAntigo, reserva), 2L);

        assertEquals(List.of(bancoAntigo, carteira, reserva), resultado);
    }

    private Categoria categoria(Long id, String nome, TipoCategoria tipo, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setAtivo(ativo);
        return categoria;
    }

    private Conta conta(Long id, String nome, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setNome(nome);
        conta.setTipo(TipoConta.OUTRO);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }
}
