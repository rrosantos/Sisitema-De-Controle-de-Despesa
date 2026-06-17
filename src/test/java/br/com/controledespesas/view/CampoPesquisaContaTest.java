package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampoPesquisaContaTest {

    @Test
    void shouldMatchNameIgnoringCaseAndOuterSpaces() {
        Conta conta = conta("Carteira principal", "Banco Azul");

        assertTrue(CampoPesquisaConta.NOME.corresponde(conta, " carteira "));
        assertTrue(CampoPesquisaConta.NOME.corresponde(conta, "PRINCIPAL"));
        assertFalse(CampoPesquisaConta.NOME.corresponde(conta, "nubank"));
    }

    @Test
    void shouldMatchInstitutionWithoutTreatingNullAsSearchableText() {
        Conta contaComInstituicao = conta("Conta digital", "Nubank");
        Conta contaSemInstituicao = conta("Carteira", null);

        assertTrue(CampoPesquisaConta.INSTITUICAO.corresponde(contaComInstituicao, " nub "));
        assertFalse(CampoPesquisaConta.INSTITUICAO.corresponde(contaSemInstituicao, "null"));
        assertFalse(CampoPesquisaConta.INSTITUICAO.corresponde(contaSemInstituicao, "nubank"));
    }

    @Test
    void shouldIgnoreBlankSearchTerm() {
        Conta conta = conta("Carteira", null);

        assertTrue(CampoPesquisaConta.NOME.corresponde(conta, "   "));
        assertTrue(CampoPesquisaConta.INSTITUICAO.corresponde(conta, ""));
    }

    private Conta conta(String nome, String instituicao) {
        Conta conta = new Conta();
        conta.setNome(nome);
        conta.setInstituicao(instituicao);
        return conta;
    }
}
