package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.CategoriaView;
import br.com.controledespesas.view.contract.DadosCategoriaForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private CategoriaView categoriaView;

    private SessaoUsuario sessaoUsuario;
    private CategoriaController categoriaController;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        sessaoUsuario.iniciar(usuario());
        categoriaController = new CategoriaController(
                categoriaService,
                sessaoUsuario,
                categoriaView,
                new ImmediateAsyncTaskExecutor()
        );
        clearInvocations(categoriaView);
    }

    @Test
    void shouldLoadCategoriesForAuthenticatedUser() throws Exception {
        List<Categoria> categorias = List.of(categoria(10L, "Salario", TipoCategoria.RECEITA, true));
        when(categoriaService.listarPorUsuario(1L)).thenReturn(categorias);

        categoriaController.carregar();

        verify(categoriaView).limparMensagem();
        verify(categoriaView).exibirCarregamento(true);
        verify(categoriaService).listarPorUsuario(1L);
        verify(categoriaView).exibirCategorias(categorias);
        verify(categoriaView).exibirCarregamento(false);
        verify(categoriaView, never()).exibirMensagemErro(any());
    }

    @Test
    void shouldDisplayEmptyStateWhenNoCategoriesExist() throws Exception {
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of());

        categoriaController.carregar();

        verify(categoriaView).exibirCategorias(List.of());
        verify(categoriaView).exibirEstadoVazio();
    }

    @Test
    void shouldCreateCategoryWhenFormIsConfirmed() throws Exception {
        DadosCategoriaForm dados = new DadosCategoriaForm("Mercado", TipoCategoria.DESPESA, "Compras do mes");
        Categoria categoria = categoria(10L, "Mercado", TipoCategoria.DESPESA, true);
        when(categoriaView.abrirFormularioCadastro()).thenReturn(Optional.of(dados));
        when(categoriaService.cadastrar(1L, "Mercado", TipoCategoria.DESPESA, "Compras do mes")).thenReturn(categoria);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));

        categoriaController.novaCategoria();

        verify(categoriaService).cadastrar(1L, "Mercado", TipoCategoria.DESPESA, "Compras do mes");
        verify(categoriaService).listarPorUsuario(1L);
        verify(categoriaView).exibirMensagemSucesso("Categoria cadastrada com sucesso.");
    }

    @Test
    void shouldShowValidationErrorWhenCreatingCategoryFails() throws Exception {
        DadosCategoriaForm dados = new DadosCategoriaForm("", TipoCategoria.RECEITA, null);
        when(categoriaView.abrirFormularioCadastro()).thenReturn(Optional.of(dados));
        when(categoriaService.cadastrar(1L, "", TipoCategoria.RECEITA, null))
                .thenThrow(new ValidacaoException("Nome da categoria e obrigatorio."));

        categoriaController.novaCategoria();

        verify(categoriaView).exibirMensagemErro("Nome da categoria e obrigatorio.");
        verify(categoriaView).exibirCarregamento(false);
    }

    @Test
    void shouldShowBusinessErrorWhenCategoryIsDuplicated() throws Exception {
        DadosCategoriaForm dados = new DadosCategoriaForm("Salario", TipoCategoria.RECEITA, null);
        when(categoriaView.abrirFormularioCadastro()).thenReturn(Optional.of(dados));
        when(categoriaService.cadastrar(1L, "Salario", TipoCategoria.RECEITA, null))
                .thenThrow(new RegraNegocioException("Ja existe uma categoria com este nome e tipo."));

        categoriaController.novaCategoria();

        verify(categoriaView).exibirMensagemErro("Ja existe uma categoria com este nome e tipo.");
    }

    @Test
    void shouldEditCategory() throws Exception {
        Categoria categoria = categoria(15L, "Lazer", TipoCategoria.DESPESA, true);
        DadosCategoriaForm dados = new DadosCategoriaForm("Lazer e viagens", TipoCategoria.DESPESA, "Passeios");
        when(categoriaView.abrirFormularioEdicao(categoria)).thenReturn(Optional.of(dados));
        when(categoriaService.atualizar(15L, 1L, "Lazer e viagens", TipoCategoria.DESPESA, "Passeios"))
                .thenReturn(categoria);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));

        categoriaController.editar(categoria);

        verify(categoriaService).atualizar(15L, 1L, "Lazer e viagens", TipoCategoria.DESPESA, "Passeios");
        verify(categoriaView).exibirMensagemSucesso("Categoria atualizada com sucesso.");
    }

    @Test
    void shouldInactivateCategory() throws Exception {
        Categoria categoria = categoria(20L, "Investimentos", TipoCategoria.RECEITA, true);
        when(categoriaView.confirmarAlteracaoStatus(categoria, false)).thenReturn(true);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));

        categoriaController.alterarStatus(categoria);

        verify(categoriaService).alterarStatus(20L, 1L, false);
        verify(categoriaView).exibirMensagemSucesso("Categoria inativada com sucesso.");
    }

    @Test
    void shouldActivateCategory() throws Exception {
        Categoria categoria = categoria(20L, "Investimentos", TipoCategoria.RECEITA, false);
        when(categoriaView.confirmarAlteracaoStatus(categoria, true)).thenReturn(true);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));

        categoriaController.alterarStatus(categoria);

        verify(categoriaService).alterarStatus(20L, 1L, true);
        verify(categoriaView).exibirMensagemSucesso("Categoria ativada com sucesso.");
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        Categoria categoria = categoria(30L, "Assinaturas", TipoCategoria.DESPESA, true);
        when(categoriaView.confirmarExclusao(categoria)).thenReturn(true);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of());

        categoriaController.excluir(categoria);

        verify(categoriaService).excluir(30L, 1L);
        verify(categoriaView).exibirMensagemSucesso("Categoria excluida com sucesso.");
    }

    @Test
    void shouldShowBlockedDeleteMessageWhenCategoryHasLinkedTransactions() throws Exception {
        Categoria categoria = categoria(30L, "Assinaturas", TipoCategoria.DESPESA, true);
        when(categoriaView.confirmarExclusao(categoria)).thenReturn(true);
        doThrow(
                new RegraNegocioException("A categoria nao pode ser excluida porque possui transacoes vinculadas.")
        ).when(categoriaService).excluir(30L, 1L);

        categoriaController.excluir(categoria);

        verify(categoriaView).exibirMensagemErro(
                "A categoria nao pode ser excluida porque possui transacoes vinculadas. Voce pode inativa-la."
        );
    }

    @Test
    void shouldShowTechnicalErrorWhenLoadingCategoriesFails() throws Exception {
        when(categoriaService.listarPorUsuario(1L)).thenThrow(new SQLException("db off"));

        categoriaController.carregar();

        verify(categoriaView).exibirMensagemErro("Nao foi possivel acessar as categorias. Tente novamente.");
        verify(categoriaView).exibirCarregamento(false);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private Categoria categoria(Long id, String nome, TipoCategoria tipo, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setUsuarioId(1L);
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setAtivo(ativo);
        return categoria;
    }
}
