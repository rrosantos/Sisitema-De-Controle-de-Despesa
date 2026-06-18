package br.com.controledespesas.view.contract;

import br.com.controledespesas.model.Conta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Define o contrato de exibicao e eventos da view de Conta.
 */
public interface ContaView {

    void exibirContas(List<Conta> contas);

    void exibirSaldos(Map<Long, BigDecimal> saldos);

    void exibirCarregamento(boolean carregando);

    void exibirMensagemSucesso(String mensagem);

    void exibirMensagemErro(String mensagem);

    void exibirEstadoVazio();

    void abrirFormularioCadastro(Consumer<DadosContaForm> aoSalvar);

    void abrirFormularioEdicao(Conta conta, Consumer<DadosContaForm> aoSalvar);

    void fecharFormulario();

    void exibirErroFormulario(String mensagem);

    boolean confirmarExclusao(Conta conta);

    boolean confirmarAlteracaoStatus(Conta conta, boolean novoStatus);

    void definirAcaoNovaConta(Runnable acao);

    void definirAcaoEditar(Consumer<Conta> acao);

    void definirAcaoAlterarStatus(Consumer<Conta> acao);

    void definirAcaoExcluir(Consumer<Conta> acao);

    void limparMensagem();
}
