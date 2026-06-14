package br.com.controledespesas.view.contract;

import javax.swing.JPanel;

public interface MainView {

    void exibirUsuario(String nome, String email);

    void adicionarPainel(String identificador, JPanel painel);

    void mostrarPainel(String identificador);

    void definirMenuAtivo(String identificador);

    void definirAcaoInicio(Runnable acao);

    void definirAcaoCategorias(Runnable acao);

    void definirAcaoContas(Runnable acao);

    void definirAcaoSair(Runnable action);

    void abrir();

    void fechar();
}
