package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class ContaListSupportTest {

    private final ContaListSupport support = new ContaListSupport();

    @Test
    void shouldSortByNameAscendingByDefault() {
        List<Conta> resultado = filtrarEOrdenar(contasBase(), Map.of(), null, "", null, null, null);

        assertNomes(resultado, "Água", "banco", "Carteira", "Poupança");
    }

    @Test
    void shouldSortByNameDescending() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                Map.of(),
                CampoPesquisaConta.NOME,
                "",
                null,
                null,
                OrdenacaoConta.NOME_DECRESCENTE
        );

        assertNomes(resultado, "Poupança", "Carteira", "banco", "Água");
    }

    @Test
    void shouldSortNameIgnoringCaseAndOuterSpaces() {
        List<Conta> contas = List.of(
                conta(1L, "  carteira", TipoConta.CARTEIRA, null, true),
                conta(2L, "Banco", TipoConta.CONTA_CORRENTE, null, true),
                conta(3L, "área", TipoConta.OUTRO, null, true)
        );

        List<Conta> resultado = filtrarEOrdenar(contas, Map.of(), null, "", null, null, OrdenacaoConta.NOME_CRESCENTE);

        assertNomes(resultado, "área", "Banco", "  carteira");
    }

    @Test
    void shouldUseInstitutionAndIdWhenNamesAreEqual() {
        List<Conta> contas = List.of(
                conta(3L, "Conta", TipoConta.OUTRO, "Zulu", true),
                conta(2L, "conta", TipoConta.OUTRO, "Alpha", true),
                conta(1L, "Conta", TipoConta.OUTRO, "Alpha", true)
        );

        List<Conta> resultado = filtrarEOrdenar(contas, Map.of(), null, "", null, null, OrdenacaoConta.NOME_CRESCENTE);

        assertIds(resultado, 1L, 2L, 3L);
    }

    @Test
    void shouldSortHighestBalanceToLowestIncludingNegativeValues() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                null,
                "",
                null,
                null,
                OrdenacaoConta.MAIOR_SALDO
        );

        assertNomes(resultado, "Poupança", "Carteira", "Água", "banco");
    }

    @Test
    void shouldSortLowestBalanceToHighestIncludingNegativeValues() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                null,
                "",
                null,
                null,
                OrdenacaoConta.MENOR_SALDO
        );

        assertNomes(resultado, "banco", "Água", "Carteira", "Poupança");
    }

    @Test
    void shouldUseNameAsTieBreakerForSameBalance() {
        List<Conta> contas = List.of(
                conta(1L, "Reserva", TipoConta.POUPANCA, null, true),
                conta(2L, "Banco", TipoConta.CONTA_CORRENTE, null, true)
        );
        Map<Long, BigDecimal> saldos = Map.of(
                1L, new BigDecimal("100.00"),
                2L, new BigDecimal("100.00")
        );

        List<Conta> resultado = filtrarEOrdenar(contas, saldos, null, "", null, null, OrdenacaoConta.MAIOR_SALDO);

        assertNomes(resultado, "Banco", "Reserva");
    }

    @Test
    void shouldPlaceAccountsWithoutKnownBalanceAfterKnownBalances() {
        List<Conta> contas = List.of(
                conta(1L, "Sem saldo", TipoConta.OUTRO, null, true),
                conta(2L, "Com saldo", TipoConta.OUTRO, null, true)
        );
        Map<Long, BigDecimal> saldos = Map.of(2L, new BigDecimal("-50.00"));

        List<Conta> resultado = filtrarEOrdenar(contas, saldos, null, "", null, null, OrdenacaoConta.MENOR_SALDO);

        assertNomes(resultado, "Com saldo", "Sem saldo");
    }

    @Test
    void shouldSortTwoAccountsWithoutKnownBalanceByName() {
        List<Conta> contas = List.of(
                conta(1L, "Zulu", TipoConta.OUTRO, null, true),
                conta(2L, "Alpha", TipoConta.OUTRO, null, true)
        );

        List<Conta> resultado = filtrarEOrdenar(contas, Map.of(), null, "", null, null, OrdenacaoConta.MAIOR_SALDO);

        assertNomes(resultado, "Alpha", "Zulu");
    }

    @Test
    void shouldSortAfterNameSearch() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                CampoPesquisaConta.NOME,
                "a",
                null,
                null,
                OrdenacaoConta.MAIOR_SALDO
        );

        assertNomes(resultado, "Poupança", "Carteira", "Água", "banco");
    }

    @Test
    void shouldSortAfterInstitutionSearch() {
        List<Conta> contas = List.of(
                conta(1L, "Conta B", TipoConta.CONTA_DIGITAL, "Nubank", true),
                conta(2L, "Conta A", TipoConta.CONTA_DIGITAL, "Nubank", true),
                conta(3L, "Conta C", TipoConta.CONTA_CORRENTE, "Banco Azul", true)
        );

        List<Conta> resultado = filtrarEOrdenar(
                contas,
                Map.of(),
                CampoPesquisaConta.INSTITUICAO,
                "nub",
                null,
                null,
                OrdenacaoConta.NOME_CRESCENTE
        );

        assertNomes(resultado, "Conta A", "Conta B");
    }

    @Test
    void shouldSortWithTypeFilter() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                null,
                "",
                TipoConta.CARTEIRA,
                null,
                OrdenacaoConta.MENOR_SALDO
        );

        assertNomes(resultado, "Carteira");
    }

    @Test
    void shouldSortWithStatusFilter() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                null,
                "",
                null,
                false,
                OrdenacaoConta.NOME_CRESCENTE
        );

        assertNomes(resultado, "banco");
    }

    @Test
    void shouldSortWhenSearchTermIsBlank() {
        List<Conta> resultado = filtrarEOrdenar(
                contasBase(),
                saldosBase(),
                CampoPesquisaConta.NOME,
                "   ",
                null,
                null,
                OrdenacaoConta.MENOR_SALDO
        );

        assertNomes(resultado, "banco", "Água", "Carteira", "Poupança");
    }

    @Test
    void shouldNotModifyOriginalList() {
        List<Conta> contas = new ArrayList<>(List.of(
                conta(1L, "Zulu", TipoConta.OUTRO, null, true),
                conta(2L, "Alpha", TipoConta.OUTRO, null, true)
        ));
        List<String> ordemOriginal = nomes(contas);

        filtrarEOrdenar(contas, Map.of(), null, "", null, null, OrdenacaoConta.NOME_CRESCENTE);

        assertIterableEquals(ordemOriginal, nomes(contas));
    }

    private List<Conta> filtrarEOrdenar(
            List<Conta> contas,
            Map<Long, BigDecimal> saldos,
            CampoPesquisaConta campoPesquisa,
            String termo,
            TipoConta tipo,
            Boolean ativo,
            OrdenacaoConta ordenacao
    ) {
        return support.filtrarEOrdenar(contas, saldos, campoPesquisa, termo, tipo, ativo, ordenacao);
    }

    private List<Conta> contasBase() {
        return List.of(
                conta(1L, "Carteira", TipoConta.CARTEIRA, null, true),
                conta(2L, "banco", TipoConta.CONTA_CORRENTE, "Banco Azul", false),
                conta(3L, "Poupança", TipoConta.POUPANCA, "Banco Azul", true),
                conta(4L, "Água", TipoConta.OUTRO, "Fonte", true)
        );
    }

    private Map<Long, BigDecimal> saldosBase() {
        return Map.of(
                1L, new BigDecimal("100.00"),
                2L, new BigDecimal("-50.00"),
                3L, new BigDecimal("5000.00"),
                4L, new BigDecimal("50.00")
        );
    }

    private Conta conta(Long id, String nome, TipoConta tipo, String instituicao, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setNome(nome);
        conta.setTipo(tipo);
        conta.setInstituicao(instituicao);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }

    private void assertNomes(List<Conta> contas, String... nomesEsperados) {
        assertIterableEquals(List.of(nomesEsperados), nomes(contas));
    }

    private void assertIds(List<Conta> contas, Long... idsEsperados) {
        assertIterableEquals(List.of(idsEsperados), contas.stream().map(Conta::getId).toList());
    }

    private List<String> nomes(List<Conta> contas) {
        return contas.stream().map(Conta::getNome).toList();
    }
}
