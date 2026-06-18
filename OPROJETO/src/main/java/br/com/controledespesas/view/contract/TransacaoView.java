package br.com.controledespesas.view.contract;

import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.Transacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Define o contrato de exibicao e eventos da view de Transacao.
 */
public interface TransacaoView {

    void exibirTransacoes(List<Transacao> transacoes);

    void exibirDadosRelacionados(Map<Long, Categoria> categorias, Map<Long, Conta> contas);

    void exibirResumo(BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldoPeriodo);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemSucesso(String mensagem);

    void exibirMensagemErro(String mensagem);

    void exibirEstadoVazio();

    void abrirFormularioCadastro(List<Categoria> categorias, List<Conta> contas, Consumer<DadosTransacaoForm> aoSalvar);

    void abrirFormularioEdicao(Transacao transacao, List<Categoria> categorias, List<Conta> contas,
                               Consumer<DadosTransacaoForm> aoSalvar);

    void fecharFormulario();

    void exibirErroFormulario(String mensagem);

    boolean confirmarExclusao(Transacao transacao);

    TransacaoFiltro obterFiltro();

    void limparFiltros();

    void definirAcaoNovaTransacao(Runnable acao);

    void definirAcaoFiltrar(Runnable acao);

    void definirAcaoLimparFiltros(Runnable acao);

    void definirAcaoEditar(Consumer<Transacao> acao);

    void definirAcaoExcluir(Consumer<Transacao> acao);

    void limparMensagem();
}
