package br.com.controledespesas.view.contract;

public interface MainView {

    void exibirUsuario(String nome, String email);

    void definirAcaoSair(Runnable action);

    void abrir();

    void fechar();
}
