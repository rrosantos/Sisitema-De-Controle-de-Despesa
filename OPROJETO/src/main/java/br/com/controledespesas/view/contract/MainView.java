package br.com.controledespesas.view.contract;

import javax.swing.JPanel;

/**
 * Define o contrato de exibicao e eventos da view de Main.
 */
public interface MainView {

    void exibirUsuario(String nome, String email);

    void adicionarPainel(String identificador, JPanel painel);

    void mostrarPainel(String identificador);

    void definirMenuAtivo(String identificador);

    void definirAcaoInicio(Runnable acao);

    void definirAcaoTransacoes(Runnable acao);

    void definirAcaoCategorias(Runnable acao);

    void definirAcaoContas(Runnable acao);

    void definirAcaoCofrinhos(Runnable acao);

    void definirAcaoUsuarios(Runnable acao);

    void definirAcaoSair(Runnable action);

    void abrir();

    void fechar();
}
