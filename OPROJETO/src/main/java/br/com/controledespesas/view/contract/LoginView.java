package br.com.controledespesas.view.contract;

/**
 * Define o contrato de exibicao e eventos da view de Login.
 */
public interface LoginView {

    String getEmail();

    char[] getSenha();

    void limparSenha();

    void limparCampos();

    void preencherEmail(String email);

    void setCarregando(boolean carregando);

    void focarEmail();

    void mostrarErro(String mensagem);

    void mostrarSucesso(String mensagem);

    void limparMensagem();

    void setEntrarAction(Runnable action);

    void setCriarContaAction(Runnable action);
}
