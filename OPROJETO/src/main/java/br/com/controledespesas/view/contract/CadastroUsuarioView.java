package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.Usuario;

import java.util.List;

/**
 * Define o contrato de exibicao e eventos da view de CadastroUsuario.
 */
public interface CadastroUsuarioView {

    String getNome();

    String getEmail();

    char[] getSenha();

    char[] getConfirmacaoSenha();

    void limparCampos();

    void limparSenhas();

    void setCarregando(boolean carregando);

    void focarNome();

    void mostrarErro(String mensagem);

    void mostrarSucesso(String mensagem);

    void limparMensagem();

    void setCadastrarAction(Runnable action);

    void setVoltarAction(Runnable action);

    default boolean suportaListagemUsuarios() {
        return false;
    }

    default void exibirUsuarios(List<Usuario> usuarios) {
        // Implementacao opcional para a tela de usuarios no menu principal.
    }
}
