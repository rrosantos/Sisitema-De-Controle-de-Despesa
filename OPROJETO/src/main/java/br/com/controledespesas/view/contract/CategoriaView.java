package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.Categoria;

import java.util.List;
import java.util.function.Consumer;

/**
 * Define o contrato de exibicao e eventos da view de Categoria.
 */
public interface CategoriaView {

    void exibirCategorias(List<Categoria> categorias);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemSucesso(String mensagem);

    void exibirMensagemErro(String mensagem);

    void exibirEstadoVazio();

    void abrirFormularioCadastro(Consumer<DadosCategoriaForm> aoSalvar);

    void abrirFormularioEdicao(Categoria categoria, Consumer<DadosCategoriaForm> aoSalvar);

    void fecharFormulario();

    void exibirErroFormulario(String mensagem);

    boolean confirmarExclusao(Categoria categoria);

    boolean confirmarAlteracaoStatus(Categoria categoria, boolean novoStatus);

    void definirAcaoNovaCategoria(Runnable acao);

    void definirAcaoEditar(Consumer<Categoria> acao);

    void definirAcaoAlterarStatus(Consumer<Categoria> acao);

    void definirAcaoExcluir(Consumer<Categoria> acao);

    void limparMensagem();
}
