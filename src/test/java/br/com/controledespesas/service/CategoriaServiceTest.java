package br.com.controledespesas.service;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaDAO categoriaDAO;

    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categoriaDAO);
    }

    @Test
    void shouldRegisterValidCategory() throws SQLException {
        when(categoriaDAO.nomeETipoExistem(1L, "Salario", TipoCategoria.RECEITA)).thenReturn(false);
        when(categoriaDAO.inserir(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria categoria = invocation.getArgument(0);
            categoria.setId(10L);
            return 10L;
        });

        Categoria categoria = categoriaService.cadastrar(1L, "  Salario  ", TipoCategoria.RECEITA, "Mensal");

        assertEquals(10L, categoria.getId());
        assertTrue(categoria.isAtivo());
    }

    @Test
    void shouldRejectDuplicateCategory() throws SQLException {
        when(categoriaDAO.nomeETipoExistem(1L, "Salario", TipoCategoria.RECEITA)).thenReturn(true);

        assertThrows(RegraNegocioException.class,
                () -> categoriaService.cadastrar(1L, "Salario", TipoCategoria.RECEITA, null));
    }

    @Test
    void shouldRejectBlankCategoryName() {
        assertThrows(ValidacaoException.class,
                () -> categoriaService.cadastrar(1L, "   ", TipoCategoria.RECEITA, null));
    }

    @Test
    void shouldRejectCategoryFromAnotherUser() throws SQLException {
        when(categoriaDAO.buscarPorId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> categoriaService.buscarPorId(99L, 1L));
    }

    @Test
    void shouldTranslateDeleteRestrictionIntoBusinessMessage() throws SQLException {
        when(categoriaDAO.buscarPorId(10L, 1L)).thenReturn(Optional.of(categoriaAtiva()));
        when(categoriaDAO.excluir(10L, 1L)).thenThrow(new SQLException("fk", "23000", 1451));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> categoriaService.excluir(10L, 1L));

        assertEquals("A categoria nao pode ser excluida porque possui transacoes vinculadas.", exception.getMessage());
    }

    private Categoria categoriaAtiva() {
        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setUsuarioId(1L);
        categoria.setNome("Salario");
        categoria.setTipo(TipoCategoria.RECEITA);
        categoria.setAtivo(true);
        return categoria;
    }
}
