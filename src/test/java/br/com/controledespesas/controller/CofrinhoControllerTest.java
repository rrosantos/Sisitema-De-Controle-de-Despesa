package br.com.controledespesas.controller;

import br.com.controledespesas.dto.CofrinhoFiltro;
import br.com.controledespesas.dto.CofrinhoResumo;
import br.com.controledespesas.dto.PrazoCofrinhoFiltro;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.MovimentacaoCofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import br.com.controledespesas.model.TipoMovimentacaoCofrinho;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.CofrinhoService;
import br.com.controledespesas.service.MovimentacaoCofrinhoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.CofrinhoView;
import br.com.controledespesas.view.contract.DadosCofrinhoForm;
import br.com.controledespesas.view.contract.DadosMovimentacaoCofrinhoForm;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CofrinhoControllerTest {

    @Mock
    private CofrinhoService cofrinhoService;

    @Mock
    private MovimentacaoCofrinhoService movimentacaoCofrinhoService;

    @Mock
    private CofrinhoView cofrinhoView;

    private SessaoUsuario sessaoUsuario;
    private CofrinhoController cofrinhoController;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        sessaoUsuario.iniciar(usuario());
        cofrinhoController = new CofrinhoController(
                cofrinhoService,
                movimentacaoCofrinhoService,
                sessaoUsuario,
                cofrinhoView,
                new ImmediateAsyncTaskExecutor()
        );
        clearInvocations(cofrinhoView);
    }

    @Test
    void shouldLoadSavingsGoalsAndApplyFilteredSummary() throws Exception {
        Cofrinho reserva = cofrinho(10L, "Reserva", new BigDecimal("1000.00"), StatusCofrinho.EM_ANDAMENTO);
        Cofrinho viagem = cofrinho(11L, "Viagem", new BigDecimal("500.00"), StatusCofrinho.CONCLUIDO);
        Cofrinho carro = cofrinho(12L, "Carro", new BigDecimal("800.00"), StatusCofrinho.CANCELADO);

        when(cofrinhoService.listarPorUsuario(1L)).thenReturn(List.of(reserva, viagem, carro));
        when(cofrinhoService.consultarValorAtual(10L, 1L)).thenReturn(new BigDecimal("200.00"));
        when(cofrinhoService.consultarValorAtual(11L, 1L)).thenReturn(new BigDecimal("700.00"));
        when(cofrinhoService.consultarValorAtual(12L, 1L)).thenReturn(new BigDecimal("300.00"));

        cofrinhoController.carregar();

        verify(cofrinhoView).exibirCofrinhos(argThat(resumos ->
                resumos.size() == 3
                        && resumos.get(0).cofrinho().getId().equals(10L)
                        && resumos.get(1).cofrinho().getId().equals(11L)
                        && resumos.get(2).cofrinho().getId().equals(12L)
        ));
        verify(cofrinhoView).exibirResumoGeral(
                argThat(total -> total.compareTo(new BigDecimal("1200.00")) == 0),
                eq(1),
                eq(1),
                eq(1)
        );

        clearInvocations(cofrinhoView);
        when(cofrinhoView.obterFiltro()).thenReturn(
                new CofrinhoFiltro(null, StatusCofrinho.CONCLUIDO, PrazoCofrinhoFiltro.TODOS)
        );

        cofrinhoController.aplicarFiltros();

        verify(cofrinhoView).exibirCofrinhos(argThat(resumos ->
                resumos.size() == 1 && resumos.get(0).cofrinho().getId().equals(11L)
        ));
        verify(cofrinhoView).exibirResumoGeral(
                argThat(total -> total.compareTo(new BigDecimal("700.00")) == 0),
                eq(0),
                eq(1),
                eq(0)
        );
    }

    @Test
    void shouldCreateSavingsGoalAndReloadList() throws Exception {
        DadosCofrinhoForm dados = new DadosCofrinhoForm(
                "Viagem",
                "Praia",
                new BigDecimal("1500.00"),
                LocalDate.of(2026, 12, 20)
        );
        Cofrinho cofrinho = cofrinho(20L, "Viagem", new BigDecimal("1500.00"), StatusCofrinho.EM_ANDAMENTO);

        doAnswer(invocation -> {
            Consumer<DadosCofrinhoForm> consumer = invocation.getArgument(0);
            consumer.accept(dados);
            return null;
        }).when(cofrinhoView).abrirFormularioCadastro(any());
        when(cofrinhoService.cadastrar(1L, "Viagem", "Praia", new BigDecimal("1500.00"), LocalDate.of(2026, 12, 20)))
                .thenReturn(cofrinho);
        when(cofrinhoService.listarPorUsuario(1L)).thenReturn(List.of(cofrinho));
        when(cofrinhoService.consultarValorAtual(20L, 1L)).thenReturn(BigDecimal.ZERO.setScale(2));

        cofrinhoController.novoCofrinho();

        verify(cofrinhoService).cadastrar(1L, "Viagem", "Praia", new BigDecimal("1500.00"), LocalDate.of(2026, 12, 20));
        verify(cofrinhoView).fecharFormularioCofrinho();
        verify(cofrinhoView).exibirMensagemSucesso("Cofrinho cadastrado com sucesso.");
        verify(cofrinhoView).exibirCofrinhos(argThat(resumos ->
                resumos.size() == 1 && resumos.get(0).cofrinho().getId().equals(20L)
        ));
    }

    @Test
    void shouldKeepWithdrawalDialogOpenWhenBusinessRuleFails() throws Exception {
        CofrinhoResumo resumo = new CofrinhoResumo(
                cofrinho(30L, "Reserva", new BigDecimal("100.00"), StatusCofrinho.EM_ANDAMENTO),
                new BigDecimal("50.00"),
                new BigDecimal("50.00")
        );
        DadosMovimentacaoCofrinhoForm dados = new DadosMovimentacaoCofrinhoForm(
                new BigDecimal("80.00"),
                LocalDate.of(2026, 6, 15),
                "Uso"
        );
        String mensagem = "O valor da retirada nao pode ser maior que o valor disponivel no cofrinho.";

        doAnswer(invocation -> {
            Consumer<DadosMovimentacaoCofrinhoForm> consumer = invocation.getArgument(1);
            consumer.accept(dados);
            return null;
        }).when(cofrinhoView).abrirFormularioRetirada(eq(resumo), any());
        when(movimentacaoCofrinhoService.retirar(30L, 1L, new BigDecimal("80.00"), LocalDate.of(2026, 6, 15), "Uso"))
                .thenThrow(new RegraNegocioException(mensagem));

        cofrinhoController.retirar(resumo);

        verify(cofrinhoView).exibirErroFormularioMovimentacao(mensagem);
        verify(cofrinhoView, never()).fecharFormularioMovimentacao();
    }

    @Test
    void shouldOpenHistoryWithUpdatedSummary() throws Exception {
        Cofrinho cofrinho = cofrinho(40L, "Notebook", new BigDecimal("3000.00"), StatusCofrinho.EM_ANDAMENTO);
        CofrinhoResumo resumoSelecionado = new CofrinhoResumo(cofrinho, BigDecimal.ZERO, BigDecimal.ZERO);
        MovimentacaoCofrinho movimentacao = movimentacao(90L, 40L, TipoMovimentacaoCofrinho.DEPOSITO, "500.00");

        when(cofrinhoService.buscarPorId(40L, 1L)).thenReturn(cofrinho);
        when(cofrinhoService.consultarValorAtual(40L, 1L)).thenReturn(new BigDecimal("500.00"));
        when(movimentacaoCofrinhoService.listarPorCofrinho(40L, 1L)).thenReturn(List.of(movimentacao));

        cofrinhoController.abrirHistorico(resumoSelecionado);

        verify(cofrinhoView).abrirHistorico(
                argThat(resumo ->
                        resumo.cofrinho().getId().equals(40L)
                                && resumo.valorAtual().compareTo(new BigDecimal("500.00")) == 0
                ),
                eq(List.of(movimentacao)),
                any()
        );
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private Cofrinho cofrinho(Long id, String nome, BigDecimal valorMeta, StatusCofrinho status) {
        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setId(id);
        cofrinho.setUsuarioId(1L);
        cofrinho.setNome(nome);
        cofrinho.setDescricao(nome + " descricao");
        cofrinho.setValorMeta(valorMeta);
        cofrinho.setStatus(status);
        return cofrinho;
    }

    private MovimentacaoCofrinho movimentacao(Long id, Long cofrinhoId, TipoMovimentacaoCofrinho tipo, String valor) {
        MovimentacaoCofrinho movimentacao = new MovimentacaoCofrinho();
        movimentacao.setId(id);
        movimentacao.setCofrinhoId(cofrinhoId);
        movimentacao.setUsuarioId(1L);
        movimentacao.setTipo(tipo);
        movimentacao.setValor(new BigDecimal(valor));
        movimentacao.setDataMovimentacao(LocalDate.of(2026, 6, 15));
        movimentacao.setObservacao("Aporte");
        return movimentacao;
    }
}
