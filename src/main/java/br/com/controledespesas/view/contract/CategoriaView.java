package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.Categoria;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CategoriaView {

    void exibirCategorias(List<Categoria> categorias);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemSucesso(String mensagem);

    void exibirMensagemErro(String mensagem);

    void exibirEstadoVazio();

    Optional<DadosCategoriaForm> abrirFormularioCadastro();

    Optional<DadosCategoriaForm> abrirFormularioEdicao(Categoria categoria);

    boolean confirmarExclusao(Categoria categoria);

    boolean confirmarAlteracaoStatus(Categoria categoria, boolean novoStatus);

    void definirAcaoNovaCategoria(Runnable acao);

    void definirAcaoEditar(Consumer<Categoria> acao);

    void definirAcaoAlterarStatus(Consumer<Categoria> acao);

    void definirAcaoExcluir(Consumer<Categoria> acao);

    void limparMensagem();
}
