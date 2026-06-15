package br.com.controledespesas.controller;

import br.com.controledespesas.dto.TransacaoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.StatusTransacao;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.TipoTransacao;
import br.com.controledespesas.model.Transacao;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.service.TransacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.DadosTransacaoForm;
import br.com.controledespesas.view.contract.TransacaoView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoControllerTest {

    @Mock
    private TransacaoService transacaoService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private ContaService contaService;

    @Mock
    private TransacaoView transacaoView;

    private SessaoUsuario sessaoUsuario;
    private TransacaoController transacaoController;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        sessaoUsuario.iniciar(usuario());
        transacaoController = new TransacaoController(
                transacaoService,
                categoriaService,
                contaService,
                sessaoUsuario,
                transacaoView,
                new ImmediateAsyncTaskExecutor()
        );
        clearInvocations(transacaoView);
    }

    @Test
    void shouldLoadInitialDataForAuthenticatedUser() throws Exception {
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA, true);
        Conta conta = conta(20L, "Banco", true);
        Transacao transacao = transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO);
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of(transacao));

        transacaoController.carregar();

        verify(categoriaService).listarPorUsuario(1L);
        verify(contaService).listarPorUsuario(1L);
        verify(transacaoService).listarPorUsuario(1L);
        verify(transacaoView).exibirDadosRelacionados(any(), any());
        verify(transacaoView).exibirTransacoes(List.of(transacao));
        verify(transacaoView).exibirResumo(
                new BigDecimal("2500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("2200.00")
        );
        verify(transacaoView).exibirCarregamento(false);
    }

    @Test
    void shouldApplyFiltersUsingViewData() throws Exception {
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA, true);
        Conta conta = conta(20L, "Banco", true);
        Transacao transacao = transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO);
        TransacaoFiltro filtro = new TransacaoFiltro(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TipoTransacao.RECEITA,
                StatusTransacao.RECEBIDO,
                10L,
                20L,
                "sal"
        );
        when(transacaoView.obterFiltro()).thenReturn(filtro);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(transacaoService.filtrar(1L, filtro)).thenReturn(List.of(transacao));
        when(transacaoService.calcularTotalReceitas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(new BigDecimal("2500.00"));
        when(transacaoService.calcularTotalDespesas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(BigDecimal.ZERO.setScale(2));
        when(transacaoService.calcularSaldoDoPeriodo(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(new BigDecimal("2500.00"));

        transacaoController.aplicarFiltros();

        verify(transacaoService).filtrar(1L, filtro);
        verify(transacaoService).calcularTotalReceitas(1L, filtro.dataInicial(), filtro.dataFinal());
        verify(transacaoService).calcularTotalDespesas(1L, filtro.dataInicial(), filtro.dataFinal());
        verify(transacaoService).calcularSaldoDoPeriodo(1L, filtro.dataInicial(), filtro.dataFinal());
    }

    @Test
    void shouldShowFilterValidationErrorWithoutCallingService() throws Exception {
        when(transacaoView.obterFiltro()).thenThrow(new ValidacaoException("A data inicial nao pode ser posterior a data final."));

        transacaoController.aplicarFiltros();

        verify(transacaoView).exibirMensagemErro("A data inicial nao pode ser posterior a data final.");
        verify(transacaoService, never()).listarPorUsuario(any());
        verify(transacaoService, never()).filtrar(any(), any());
    }

    @Test
    void shouldClearFiltersAndReloadAllTransactions() throws Exception {
        prepararCarregamentoBase(
                List.of(categoria(10L, "Salario", TipoCategoria.RECEITA, true)),
                List.of(conta(20L, "Banco", true)),
                List.of(transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO))
        );

        transacaoController.limparFiltros();

        verify(transacaoView).limparFiltros();
        verify(transacaoService).listarPorUsuario(1L);
    }

    @Test
    void shouldCreateTransactionAndCloseFormOnSuccess() throws Exception {
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA, true);
        Conta conta = conta(20L, "Banco", true);
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of());
        transacaoController.carregar();
        clearInvocations(transacaoView, transacaoService, categoriaService, contaService);

        DadosTransacaoForm dados = new DadosTransacaoForm(
                TipoTransacao.RECEITA,
                "Salario",
                new BigDecimal("2500.00"),
                LocalDate.of(2026, 6, 10),
                10L,
                20L,
                StatusTransacao.RECEBIDO,
                "Junho"
        );
        doAnswer(invocation -> {
            Consumer<DadosTransacaoForm> consumer = invocation.getArgument(2);
            consumer.accept(dados);
            return null;
        }).when(transacaoView).abrirFormularioCadastro(any(), any(), any());
        when(transacaoService.cadastrar(1L, 10L, 20L, TipoTransacao.RECEITA, "Salario",
                new BigDecimal("2500.00"), LocalDate.of(2026, 6, 10), StatusTransacao.RECEBIDO, "Junho"))
                .thenReturn(transacao(40L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO));
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of(
                transacao(40L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO)
        ));

        transacaoController.novaTransacao();

        verify(transacaoService).cadastrar(
                1L, 10L, 20L, TipoTransacao.RECEITA, "Salario",
                new BigDecimal("2500.00"), LocalDate.of(2026, 6, 10), StatusTransacao.RECEBIDO, "Junho"
        );
        verify(transacaoView).fecharFormulario();
        verify(transacaoView).exibirMensagemSucesso("Transacao cadastrada com sucesso.");
    }

    @Test
    void shouldKeepTransactionFormOpenWhenBusinessRuleFails() throws Exception {
        Categoria categoria = categoria(10L, "Mercado", TipoCategoria.DESPESA, true);
        Conta conta = conta(20L, "Banco", true);
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of());
        transacaoController.carregar();
        clearInvocations(transacaoView, transacaoService, categoriaService, contaService);

        DadosTransacaoForm dados = new DadosTransacaoForm(
                TipoTransacao.DESPESA,
                "Mercado",
                new BigDecimal("300.00"),
                LocalDate.of(2026, 6, 10),
                10L,
                20L,
                StatusTransacao.RECEBIDO,
                null
        );
        doAnswer(invocation -> {
            Consumer<DadosTransacaoForm> consumer = invocation.getArgument(2);
            consumer.accept(dados);
            return null;
        }).when(transacaoView).abrirFormularioCadastro(any(), any(), any());
        when(transacaoService.cadastrar(1L, 10L, 20L, TipoTransacao.DESPESA, "Mercado",
                new BigDecimal("300.00"), LocalDate.of(2026, 6, 10), StatusTransacao.RECEBIDO, null))
                .thenThrow(new RegraNegocioException("O status informado nao e compativel com o tipo da transacao."));

        transacaoController.novaTransacao();

        verify(transacaoView).exibirErroFormulario("O status informado nao e compativel com o tipo da transacao.");
        verify(transacaoView, never()).fecharFormulario();
    }

    @Test
    void shouldEditTransaction() throws Exception {
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA, true);
        Conta conta = conta(20L, "Banco", true);
        Transacao transacao = transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.PENDENTE);
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of(transacao));
        transacaoController.carregar();
        clearInvocations(transacaoView, transacaoService, categoriaService, contaService);

        DadosTransacaoForm dados = new DadosTransacaoForm(
                TipoTransacao.RECEITA,
                "Salario reajustado",
                new BigDecimal("2700.00"),
                LocalDate.of(2026, 6, 11),
                10L,
                20L,
                StatusTransacao.RECEBIDO,
                "Ajuste"
        );
        doAnswer(invocation -> {
            Consumer<DadosTransacaoForm> consumer = invocation.getArgument(3);
            consumer.accept(dados);
            return null;
        }).when(transacaoView).abrirFormularioEdicao(eq(transacao), any(), any(), any());
        when(transacaoService.atualizar(
                30L, 1L, 10L, 20L, TipoTransacao.RECEITA, "Salario reajustado",
                new BigDecimal("2700.00"), LocalDate.of(2026, 6, 11), StatusTransacao.RECEBIDO, "Ajuste"
        )).thenReturn(transacao);
        prepararCarregamentoBase(List.of(categoria), List.of(conta), List.of(
                transacao(30L, "Salario reajustado", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO)
        ));

        transacaoController.editar(transacao);

        verify(transacaoService).atualizar(
                30L, 1L, 10L, 20L, TipoTransacao.RECEITA, "Salario reajustado",
                new BigDecimal("2700.00"), LocalDate.of(2026, 6, 11), StatusTransacao.RECEBIDO, "Ajuste"
        );
        verify(transacaoView).fecharFormulario();
        verify(transacaoView).exibirMensagemSucesso("Transacao atualizada com sucesso.");
    }

    @Test
    void shouldDeleteTransactionUsingLastAppliedFilter() throws Exception {
        Categoria categoria = categoria(10L, "Salario", TipoCategoria.RECEITA, true);
        Conta conta = conta(20L, "Banco", true);
        Transacao transacao = transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO);
        TransacaoFiltro filtro = new TransacaoFiltro(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                TipoTransacao.RECEITA,
                null,
                null,
                null,
                null
        );
        when(transacaoView.obterFiltro()).thenReturn(filtro);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(transacaoService.filtrar(1L, filtro)).thenReturn(List.of(transacao));
        when(transacaoService.calcularTotalReceitas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(new BigDecimal("2500.00"));
        when(transacaoService.calcularTotalDespesas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(BigDecimal.ZERO.setScale(2));
        when(transacaoService.calcularSaldoDoPeriodo(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(new BigDecimal("2500.00"));
        transacaoController.aplicarFiltros();
        clearInvocations(transacaoView, transacaoService, categoriaService, contaService);

        when(transacaoView.confirmarExclusao(transacao)).thenReturn(true);
        when(categoriaService.listarPorUsuario(1L)).thenReturn(List.of(categoria));
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(transacaoService.filtrar(1L, filtro)).thenReturn(List.of());
        when(transacaoService.calcularTotalReceitas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(BigDecimal.ZERO.setScale(2));
        when(transacaoService.calcularTotalDespesas(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(BigDecimal.ZERO.setScale(2));
        when(transacaoService.calcularSaldoDoPeriodo(1L, filtro.dataInicial(), filtro.dataFinal()))
                .thenReturn(BigDecimal.ZERO.setScale(2));

        transacaoController.excluir(transacao);

        verify(transacaoService).excluir(30L, 1L);
        verify(transacaoService).filtrar(1L, filtro);
        verify(transacaoView).exibirMensagemSucesso("Transacao excluida com sucesso.");
    }

    @Test
    void shouldNotDeleteWhenUserCancels() throws Exception {
        Transacao transacao = transacao(30L, "Salario", TipoTransacao.RECEITA, StatusTransacao.RECEBIDO);
        when(transacaoView.confirmarExclusao(transacao)).thenReturn(false);

        transacaoController.excluir(transacao);

        verify(transacaoService, never()).excluir(any(), any());
    }

    @Test
    void shouldShowTechnicalLoadError() throws Exception {
        when(categoriaService.listarPorUsuario(1L)).thenThrow(new SQLException("db off"));

        transacaoController.carregar();

        verify(transacaoView).exibirMensagemErro("Nao foi possivel carregar as transacoes. Tente novamente.");
        verify(transacaoView).exibirCarregamento(false);
    }

    private void prepararCarregamentoBase(List<Categoria> categorias, List<Conta> contas, List<Transacao> transacoes)
            throws Exception {
        when(categoriaService.listarPorUsuario(1L)).thenReturn(categorias);
        when(contaService.listarPorUsuario(1L)).thenReturn(contas);
        when(transacaoService.listarPorUsuario(1L)).thenReturn(transacoes);
        when(transacaoService.calcularTotalReceitas(1L, null, null)).thenReturn(new BigDecimal("2500.00"));
        when(transacaoService.calcularTotalDespesas(1L, null, null)).thenReturn(new BigDecimal("300.00"));
        when(transacaoService.calcularSaldoDoPeriodo(1L, null, null)).thenReturn(new BigDecimal("2200.00"));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private Categoria categoria(Long id, String nome, TipoCategoria tipo, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setUsuarioId(1L);
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setAtivo(ativo);
        return categoria;
    }

    private Conta conta(Long id, String nome, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setUsuarioId(1L);
        conta.setNome(nome);
        conta.setTipo(TipoConta.CONTA_CORRENTE);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }

    private Transacao transacao(Long id, String descricao, TipoTransacao tipo, StatusTransacao status) {
        Transacao transacao = new Transacao();
        transacao.setId(id);
        transacao.setUsuarioId(1L);
        transacao.setCategoriaId(10L);
        transacao.setContaId(20L);
        transacao.setDescricao(descricao);
        transacao.setTipo(tipo);
        transacao.setStatus(status);
        transacao.setValor(new BigDecimal("2500.00"));
        transacao.setDataTransacao(LocalDate.of(2026, 6, 10));
        return transacao;
    }
}
