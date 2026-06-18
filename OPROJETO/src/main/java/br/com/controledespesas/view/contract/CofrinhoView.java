package br.com.controledespesas.view.contract;

import br.com.controledespesas.dto.CofrinhoFiltro;
import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/**
 * Define o contrato de exibicao e eventos da view de Cofrinho.
 */
public interface CofrinhoView {

    void exibirCofrinhos(List<CofrinhoResumo> cofrinhos);

    void exibirResumoGeral(BigDecimal totalGuardado, int metasEmAndamento, int metasConcluidas, int metasCanceladas);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemSucesso(String mensagem);

    void exibirMensagemErro(String mensagem);

    void exibirEstadoVazio();

    void abrirFormularioCadastro(Consumer<DadosCofrinhoForm> aoSalvar);

    void abrirFormularioEdicao(Cofrinho cofrinho, Consumer<DadosCofrinhoForm> aoSalvar);

    void fecharFormularioCofrinho();

    void exibirErroFormularioCofrinho(String mensagem);

    void abrirFormularioDeposito(CofrinhoResumo resumo, Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar);

    void abrirFormularioRetirada(CofrinhoResumo resumo, Consumer<DadosMovimentacaoCofrinhoForm> aoSalvar);

    void fecharFormularioMovimentacao();

    void exibirErroFormularioMovimentacao(String mensagem);

    void abrirHistorico(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes,
                        Consumer<MovimentacaoCofrinho> aoExcluir);

    void atualizarHistorico(CofrinhoResumo resumo, List<MovimentacaoCofrinho> movimentacoes);

    void fecharHistorico();

    void exibirErroHistorico(String mensagem);

    boolean confirmarCancelamento(Cofrinho cofrinho);

    boolean confirmarReativacao(Cofrinho cofrinho);

    boolean confirmarExclusao(Cofrinho cofrinho);

    boolean confirmarExclusaoMovimentacao(MovimentacaoCofrinho movimentacao);

    CofrinhoFiltro obterFiltro();

    void limparFiltros();

    void definirAcaoNovoCofrinho(Runnable acao);

    void definirAcaoFiltrar(Runnable acao);

    void definirAcaoLimparFiltros(Runnable acao);

    void definirAcaoEditar(Consumer<CofrinhoResumo> acao);

    void definirAcaoCancelar(Consumer<CofrinhoResumo> acao);

    void definirAcaoReativar(Consumer<CofrinhoResumo> acao);

    void definirAcaoExcluir(Consumer<CofrinhoResumo> acao);

    void definirAcaoDepositar(Consumer<CofrinhoResumo> acao);

    void definirAcaoRetirar(Consumer<CofrinhoResumo> acao);

    void definirAcaoHistorico(Consumer<CofrinhoResumo> acao);

    void limparMensagem();
}
