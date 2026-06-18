package br.com.controledespesas.view.contract;

import br.com.controledespesas.dto.DashboardResumo;

import java.time.LocalDate;

/**
 * Define o contrato de exibicao e eventos da view de Dashboard.
 */
public interface DashboardView {

    void exibirResumo(DashboardResumo resumo);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemErro(String mensagem);

    void limparErro();

    LocalDate obterDataInicial();

    LocalDate obterDataFinal();

    void definirPeriodo(LocalDate dataInicial, LocalDate dataFinal);

    void definirAcaoAplicarFiltro(Runnable acao);

    void definirAcaoAtualizar(Runnable acao);

    void definirAcaoTentarNovamente(Runnable acao);

    void definirAcaoMesAtual(Runnable acao);

    void definirAcaoMesAnterior(Runnable acao);

    void definirAcaoUltimosTrintaDias(Runnable acao);

    void definirAcaoEsteAno(Runnable acao);

    void definirAcaoLimparPeriodo(Runnable acao);

    void definirAcaoAbrirTransacoes(Runnable acao);

    void definirAcaoAbrirContas(Runnable acao);

    void definirAcaoAbrirCofrinhos(Runnable acao);
}
