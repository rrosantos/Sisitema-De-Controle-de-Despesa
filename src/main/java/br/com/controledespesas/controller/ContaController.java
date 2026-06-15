package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.ContaView;
import br.com.controledespesas.view.contract.DadosContaForm;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContaController {

    private static final Logger LOGGER = Logger.getLogger(ContaController.class.getName());
    private static final String MENSAGEM_ERRO_TECNICO =
            "Nao foi possivel acessar as contas. Tente novamente.";
    private static final String MENSAGEM_ERRO_SALVAR =
            "Nao foi possivel salvar a conta. Tente novamente.";
    private static final String MENSAGEM_EXCLUSAO_BLOQUEADA =
            "A conta nao pode ser excluida porque possui transacoes vinculadas. Voce pode inativa-la.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Conta cadastrada com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Conta atualizada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Conta excluida com sucesso.";
    private static final String MENSAGEM_ATIVACAO_SUCESSO = "Conta ativada com sucesso.";
    private static final String MENSAGEM_INATIVACAO_SUCESSO = "Conta inativada com sucesso.";

    private final ContaService contaService;
    private final SessaoUsuario sessaoUsuario;
    private final ContaView contaView;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public ContaController(ContaService contaService, SessaoUsuario sessaoUsuario,
                           ContaView contaView, AsyncTaskExecutor asyncTaskExecutor) {
        this.contaService = Objects.requireNonNull(contaService, "contaService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.contaView = Objects.requireNonNull(contaView, "contaView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        configurarAcoes();
    }

    public void carregar() {
        contaView.limparMensagem();
        executarOperacao(this::carregarResultadoInicial, false);
    }

    public void novaConta() {
        contaView.limparMensagem();
        contaView.abrirFormularioCadastro(dados -> executarOperacaoFormulario(() -> cadastrarConta(dados)));
    }

    public void editar(Conta conta) {
        if (conta == null) {
            return;
        }

        contaView.limparMensagem();
        contaView.abrirFormularioEdicao(
                conta,
                dados -> executarOperacaoFormulario(() -> atualizarConta(conta, dados))
        );
    }

    public void alterarStatus(Conta conta) {
        if (conta == null) {
            return;
        }

        boolean novoStatus = !conta.isAtivo();
        if (!contaView.confirmarAlteracaoStatus(conta, novoStatus)) {
            return;
        }

        executarOperacao(() -> alterarStatusConta(conta, novoStatus), true);
    }

    public void excluir(Conta conta) {
        if (conta == null || !contaView.confirmarExclusao(conta)) {
            return;
        }

        executarOperacao(() -> excluirConta(conta), true);
    }

    private void configurarAcoes() {
        contaView.definirAcaoNovaConta(this::novaConta);
        contaView.definirAcaoEditar(this::editar);
        contaView.definirAcaoAlterarStatus(this::alterarStatus);
        contaView.definirAcaoExcluir(this::excluir);
    }

    private void executarOperacao(Callable<ContaResultado> operacao, boolean limparMensagemAntes) {
        if (limparMensagemAntes) {
            contaView.limparMensagem();
        }

        contaView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                this::aplicarResultado,
                this::tratarErroPainel,
                () -> contaView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormulario(Callable<ContaResultado> operacao) {
        contaView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    contaView.fecharFormulario();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormulario,
                () -> contaView.exibirCarregamento(false)
        );
    }

    private ContaResultado carregarResultadoInicial() throws SQLException {
        return carregarDadosUsuarioAtual(null);
    }

    private ContaResultado cadastrarConta(DadosContaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        contaService.cadastrar(usuarioId, dados.nome(), dados.tipo(), dados.instituicao(), dados.saldoInicial());
        return carregarDadosUsuarioAtual(MENSAGEM_CADASTRO_SUCESSO);
    }

    private ContaResultado atualizarConta(Conta conta, DadosContaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        contaService.atualizar(
                conta.getId(),
                usuarioId,
                dados.nome(),
                dados.tipo(),
                dados.instituicao(),
                dados.saldoInicial()
        );
        return carregarDadosUsuarioAtual(MENSAGEM_EDICAO_SUCESSO);
    }

    private ContaResultado alterarStatusConta(Conta conta, boolean novoStatus) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        contaService.alterarStatus(conta.getId(), usuarioId, novoStatus);
        String mensagem = novoStatus ? MENSAGEM_ATIVACAO_SUCESSO : MENSAGEM_INATIVACAO_SUCESSO;
        return carregarDadosUsuarioAtual(mensagem);
    }

    private ContaResultado excluirConta(Conta conta) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        contaService.excluir(conta.getId(), usuarioId);
        return carregarDadosUsuarioAtual(MENSAGEM_EXCLUSAO_SUCESSO);
    }

    private ContaResultado carregarDadosUsuarioAtual(String mensagemSucesso) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        List<Conta> contas = contaService.listarPorUsuario(usuarioId);
        Map<Long, BigDecimal> saldos = new LinkedHashMap<>();

        for (Conta conta : contas) {
            if (conta.getId() == null) {
                continue;
            }

            try {
                saldos.put(conta.getId(), contaService.consultarSaldoAtual(conta.getId(), usuarioId));
            } catch (SQLException | RegraNegocioException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Nao foi possivel consultar o saldo atual da conta de id " + conta.getId() + ".",
                        exception
                );
            }
        }

        return new ContaResultado(contas, saldos, mensagemSucesso);
    }

    private void aplicarResultado(ContaResultado resultado) {
        contaView.exibirContas(resultado.contas());
        contaView.exibirSaldos(resultado.saldos());
        if (resultado.contas().isEmpty()) {
            contaView.exibirEstadoVazio();
        }
        if (resultado.mensagemSucesso() != null && !resultado.mensagemSucesso().isBlank()) {
            contaView.exibirMensagemSucesso(resultado.mensagemSucesso());
        }
    }

    private void tratarErroPainel(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            contaView.exibirMensagemErro(mapearMensagemNegocio(throwable.getMessage()));
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha tecnica ao processar contas.", throwable);
            contaView.exibirMensagemErro(MENSAGEM_ERRO_TECNICO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado no modulo de contas.", throwable);
        contaView.exibirMensagemErro(MENSAGEM_ERRO_TECNICO);
    }

    private void tratarErroFormulario(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            contaView.exibirErroFormulario(mapearMensagemNegocio(throwable.getMessage()));
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha tecnica ao salvar conta.", throwable);
            contaView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao salvar conta.", throwable);
        contaView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
    }

    private String mapearMensagemNegocio(String mensagemOriginal) {
        if (mensagemOriginal != null && mensagemOriginal.contains("transacoes vinculadas")) {
            return MENSAGEM_EXCLUSAO_BLOQUEADA;
        }
        return mensagemOriginal != null && !mensagemOriginal.isBlank() ? mensagemOriginal : MENSAGEM_ERRO_TECNICO;
    }

    private record ContaResultado(List<Conta> contas, Map<Long, BigDecimal> saldos, String mensagemSucesso) {
    }
}
