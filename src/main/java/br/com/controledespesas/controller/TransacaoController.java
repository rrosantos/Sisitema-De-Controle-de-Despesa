package br.com.controledespesas.controller;

import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.Transacao;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.service.TransacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.DadosTransacaoForm;
import br.com.controledespesas.view.contract.TransacaoView;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransacaoController {

    private static final Logger LOGGER = Logger.getLogger(TransacaoController.class.getName());
    private static final String MENSAGEM_ERRO_LISTAGEM =
            "Nao foi possivel carregar as transacoes. Tente novamente.";
    private static final String MENSAGEM_ERRO_SALVAR =
            "Nao foi possivel salvar a transacao. Tente novamente.";
    private static final String MENSAGEM_ERRO_EXCLUIR =
            "Nao foi possivel excluir a transacao. Tente novamente.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Transacao cadastrada com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Transacao atualizada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Transacao excluida com sucesso.";

    private final TransacaoService transacaoService;
    private final CategoriaService categoriaService;
    private final ContaService contaService;
    private final SessaoUsuario sessaoUsuario;
    private final TransacaoView transacaoView;
    private final AsyncTaskExecutor asyncTaskExecutor;

    private List<Categoria> categoriasUsuario = List.of();
    private List<Conta> contasUsuario = List.of();
    private TransacaoFiltro ultimoFiltroAplicado = new TransacaoFiltro();

    public TransacaoController(TransacaoService transacaoService, CategoriaService categoriaService,
                               ContaService contaService, SessaoUsuario sessaoUsuario,
                               TransacaoView transacaoView, AsyncTaskExecutor asyncTaskExecutor) {
        this.transacaoService = Objects.requireNonNull(transacaoService, "transacaoService nao pode ser nulo.");
        this.categoriaService = Objects.requireNonNull(categoriaService, "categoriaService nao pode ser nulo.");
        this.contaService = Objects.requireNonNull(contaService, "contaService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.transacaoView = Objects.requireNonNull(transacaoView, "transacaoView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        configurarAcoes();
    }

    public void carregar() {
        transacaoView.limparMensagem();
        carregarComFiltro(ultimoFiltroAplicado, null);
    }

    public void aplicarFiltros() {
        try {
            TransacaoFiltro filtro = transacaoView.obterFiltro();
            transacaoView.limparMensagem();
            carregarComFiltro(filtro, null);
        } catch (ValidacaoException exception) {
            transacaoView.exibirMensagemErro(exception.getMessage());
        }
    }

    public void limparFiltros() {
        transacaoView.limparFiltros();
        transacaoView.limparMensagem();
        carregarComFiltro(new TransacaoFiltro(), null);
    }

    public void novaTransacao() {
        transacaoView.limparMensagem();
        transacaoView.abrirFormularioCadastro(
                categoriasUsuario.stream().filter(Categoria::isAtivo).toList(),
                contasUsuario.stream().filter(Conta::isAtivo).toList(),
                dados -> executarOperacaoFormulario(() -> cadastrarTransacao(dados))
        );
    }

    public void editar(Transacao transacao) {
        if (transacao == null) {
            return;
        }

        transacaoView.limparMensagem();
        transacaoView.abrirFormularioEdicao(
                transacao,
                categoriasUsuario,
                contasUsuario,
                dados -> executarOperacaoFormulario(() -> atualizarTransacao(transacao, dados))
        );
    }

    public void excluir(Transacao transacao) {
        if (transacao == null || !transacaoView.confirmarExclusao(transacao)) {
            return;
        }

        transacaoView.limparMensagem();
        executarOperacaoExclusao(() -> excluirTransacao(transacao));
    }

    private void configurarAcoes() {
        transacaoView.definirAcaoNovaTransacao(this::novaTransacao);
        transacaoView.definirAcaoFiltrar(this::aplicarFiltros);
        transacaoView.definirAcaoLimparFiltros(this::limparFiltros);
        transacaoView.definirAcaoEditar(this::editar);
        transacaoView.definirAcaoExcluir(this::excluir);
    }

    private void carregarComFiltro(TransacaoFiltro filtro, String mensagemSucesso) {
        ultimoFiltroAplicado = filtro != null ? filtro : new TransacaoFiltro();
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                () -> carregarDados(ultimoFiltroAplicado, mensagemSucesso),
                this::aplicarResultado,
                this::tratarErroListagem,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormulario(Callable<ResultadoCarregamentoTransacoes> operacao) {
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    transacaoView.fecharFormulario();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormulario,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoExclusao(Callable<ResultadoCarregamentoTransacoes> operacao) {
        transacaoView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                this::aplicarResultado,
                this::tratarErroExclusao,
                () -> transacaoView.exibirCarregamento(false)
        );
    }

    private ResultadoCarregamentoTransacoes cadastrarTransacao(DadosTransacaoForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        transacaoService.cadastrar(
                usuarioId,
                dados.categoriaId(),
                dados.contaId(),
                dados.tipo(),
                dados.descricao(),
                dados.valor(),
                dados.dataTransacao(),
                dados.status(),
                dados.observacoes()
        );
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_CADASTRO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes atualizarTransacao(Transacao transacao, DadosTransacaoForm dados)
            throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        transacaoService.atualizar(
                transacao.getId(),
                usuarioId,
                dados.categoriaId(),
                dados.contaId(),
                dados.tipo(),
                dados.descricao(),
                dados.valor(),
                dados.dataTransacao(),
                dados.status(),
                dados.observacoes()
        );
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_EDICAO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes excluirTransacao(Transacao transacao) throws SQLException {
        transacaoService.excluir(transacao.getId(), sessaoUsuario.exigirUsuarioId());
        return carregarDados(ultimoFiltroAplicado, MENSAGEM_EXCLUSAO_SUCESSO);
    }

    private ResultadoCarregamentoTransacoes carregarDados(TransacaoFiltro filtro, String mensagemSucesso) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        List<Categoria> categorias = categoriaService.listarPorUsuario(usuarioId);
        List<Conta> contas = contaService.listarPorUsuario(usuarioId);
        List<Transacao> transacoes = filtro != null && filtro.possuiFiltros()
                ? transacaoService.filtrar(usuarioId, filtro)
                : transacaoService.listarPorUsuario(usuarioId);
        BigDecimal totalReceitas = transacaoService.calcularTotalReceitas(usuarioId, filtro.dataInicial(), filtro.dataFinal());
        BigDecimal totalDespesas = transacaoService.calcularTotalDespesas(usuarioId, filtro.dataInicial(), filtro.dataFinal());
        BigDecimal saldoPeriodo = transacaoService.calcularSaldoDoPeriodo(usuarioId, filtro.dataInicial(), filtro.dataFinal());

        return new ResultadoCarregamentoTransacoes(
                transacoes,
                categorias,
                contas,
                totalReceitas,
                totalDespesas,
                saldoPeriodo,
                mensagemSucesso
        );
    }

    private void aplicarResultado(ResultadoCarregamentoTransacoes resultado) {
        categoriasUsuario = List.copyOf(resultado.categorias());
        contasUsuario = List.copyOf(resultado.contas());
        transacaoView.exibirDadosRelacionados(mapearCategorias(resultado.categorias()), mapearContas(resultado.contas()));
        transacaoView.exibirTransacoes(resultado.transacoes());
        transacaoView.exibirResumo(resultado.totalReceitas(), resultado.totalDespesas(), resultado.saldoPeriodo());
        if (resultado.transacoes().isEmpty()) {
            transacaoView.exibirEstadoVazio();
        }
        if (resultado.mensagemSucesso() != null && !resultado.mensagemSucesso().isBlank()) {
            transacaoView.exibirMensagemSucesso(resultado.mensagemSucesso());
        }
    }

    private void tratarErroListagem(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao carregar transacoes.", throwable);
            transacaoView.exibirMensagemErro(MENSAGEM_ERRO_LISTAGEM);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao carregar transacoes.", throwable);
        transacaoView.exibirMensagemErro(MENSAGEM_ERRO_LISTAGEM);
    }

    private void tratarErroFormulario(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirErroFormulario(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao salvar transacao.", throwable);
            transacaoView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao salvar transacao.", throwable);
        transacaoView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
    }

    private void tratarErroExclusao(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            transacaoView.exibirMensagemErro(throwable.getMessage());
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha ao excluir transacao.", throwable);
            transacaoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao excluir transacao.", throwable);
        transacaoView.exibirMensagemErro(MENSAGEM_ERRO_EXCLUIR);
    }

    private Map<Long, Categoria> mapearCategorias(List<Categoria> categorias) {
        Map<Long, Categoria> categoriasMap = new LinkedHashMap<>();
        for (Categoria categoria : categorias) {
            if (categoria.getId() != null) {
                categoriasMap.put(categoria.getId(), categoria);
            }
        }
        return categoriasMap;
    }

    private Map<Long, Conta> mapearContas(List<Conta> contas) {
        Map<Long, Conta> contasMap = new LinkedHashMap<>();
        for (Conta conta : contas) {
            if (conta.getId() != null) {
                contasMap.put(conta.getId(), conta);
            }
        }
        return contasMap;
    }

    private record ResultadoCarregamentoTransacoes(
            List<Transacao> transacoes,
            List<Categoria> categorias,
            List<Conta> contas,
            BigDecimal totalReceitas,
            BigDecimal totalDespesas,
            BigDecimal saldoPeriodo,
            String mensagemSucesso
    ) {
    }
}
