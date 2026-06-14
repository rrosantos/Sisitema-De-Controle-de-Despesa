package br.com.controledespesas.dto;

import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoTransacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransacaoFiltroTest {

    @Test
    void shouldCreateEmptyFilterByDefault() {
        TransacaoFiltro filtro = new TransacaoFiltro();

        assertAll(
                () -> assertNull(filtro.dataInicial()),
                () -> assertNull(filtro.dataFinal()),
                () -> assertNull(filtro.tipo()),
                () -> assertNull(filtro.status()),
                () -> assertNull(filtro.categoriaId()),
                () -> assertNull(filtro.contaId()),
                () -> assertNull(filtro.descricao()),
                () -> assertFalse(filtro.possuiFiltros())
        );
    }

    @Test
    void shouldNormalizeDescriptionAndReportWhenFiltersExist() {
        TransacaoFiltro filtro = new TransacaoFiltro(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                TipoTransacao.RECEITA,
                StatusTransacao.RECEBIDO,
                10L,
                20L,
                "  salario  "
        );

        assertAll(
                () -> assertTrue(filtro.possuiFiltros()),
                () -> assertTrue(filtro.dataInicial().isBefore(filtro.dataFinal())),
                () -> assertTrue(filtro.tipo() == TipoTransacao.RECEITA),
                () -> assertTrue(filtro.status() == StatusTransacao.RECEBIDO),
                () -> assertTrue(filtro.categoriaId().equals(10L)),
                () -> assertTrue(filtro.contaId().equals(20L)),
                () -> assertTrue("salario".equals(filtro.descricao()))
        );
    }

    @Test
    void shouldTreatBlankDescriptionAsNull() {
        TransacaoFiltro filtro = new TransacaoFiltro(null, null, null, null, null, null, "   ");
        assertNull(filtro.descricao());
    }
}
