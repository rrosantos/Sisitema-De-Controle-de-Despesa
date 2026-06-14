package br.com.controledespesas.view.contract;

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
}
