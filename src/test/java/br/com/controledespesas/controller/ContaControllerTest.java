package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.ContaView;
import br.com.controledespesas.view.contract.DadosContaForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaControllerTest {

    @Mock
    private ContaService contaService;

    @Mock
    private ContaView contaView;

    @Mock
    private DashboardRefreshNotifier dashboardRefreshNotifier;

    private SessaoUsuario sessaoUsuario;
    private ContaController contaController;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        sessaoUsuario.iniciar(usuario());
        contaController = new ContaController(
                contaService,
                sessaoUsuario,
                contaView,
                new ImmediateAsyncTaskExecutor(),
                dashboardRefreshNotifier
        );
        clearInvocations(contaView);
    }

    @Test
    void shouldLoadAccountsAndBalancesForAuthenticatedUser() throws Exception {
        Conta carteira = conta(10L, "Carteira", TipoConta.CARTEIRA, true);
        Conta banco = conta(11L, "Banco", TipoConta.CONTA_CORRENTE, true);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(carteira, banco));
        when(contaService.consultarSaldoAtual(10L, 1L)).thenReturn(new BigDecimal("50.00"));
        when(contaService.consultarSaldoAtual(11L, 1L)).thenReturn(new BigDecimal("1250.75"));

        contaController.carregar();

        verify(contaView).limparMensagem();
        verify(contaService).listarPorUsuario(1L);
        verify(contaService).consultarSaldoAtual(10L, 1L);
        verify(contaService).consultarSaldoAtual(11L, 1L);
        verify(contaView).exibirContas(List.of(carteira, banco));
        verify(contaView).exibirSaldos(Map.of(
                10L, new BigDecimal("50.00"),
                11L, new BigDecimal("1250.75")
        ));
        verify(contaView).exibirCarregamento(false);
    }

    @Test
    void shouldKeepScreenLoadedWhenOneBalanceFails() throws Exception {
        Conta carteira = conta(10L, "Carteira", TipoConta.CARTEIRA, true);
        Conta banco = conta(11L, "Banco", TipoConta.CONTA_CORRENTE, true);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(carteira, banco));
        when(contaService.consultarSaldoAtual(10L, 1L)).thenReturn(new BigDecimal("50.00"));
        when(contaService.consultarSaldoAtual(11L, 1L)).thenThrow(new SQLException("falha pontual"));

        contaController.carregar();

        verify(contaView).exibirContas(List.of(carteira, banco));
        verify(contaView).exibirSaldos(Map.of(10L, new BigDecimal("50.00")));
        verify(contaView, never()).exibirMensagemErro(any());
    }

    @Test
    void shouldCreateAccountWhenFormIsConfirmed() throws Exception {
        DadosContaForm dados = new DadosContaForm(
                "Carteira",
                TipoConta.CARTEIRA,
                null,
                new BigDecimal("0.00")
        );
        Conta conta = conta(20L, "Carteira", TipoConta.CARTEIRA, true);
        doAnswer(invocation -> {
            Consumer<DadosContaForm> consumer = invocation.getArgument(0);
            consumer.accept(dados);
            return null;
        }).when(contaView).abrirFormularioCadastro(any());
        when(contaService.cadastrar(1L, "Carteira", TipoConta.CARTEIRA, null, new BigDecimal("0.00"))).thenReturn(conta);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(contaService.consultarSaldoAtual(20L, 1L)).thenReturn(new BigDecimal("0.00"));

        contaController.novaConta();

        verify(contaService).cadastrar(1L, "Carteira", TipoConta.CARTEIRA, null, new BigDecimal("0.00"));
        verify(contaView).fecharFormulario();
        verify(contaView).exibirMensagemSucesso("Conta cadastrada com sucesso.");
        verify(dashboardRefreshNotifier).marcarDashboardComoDesatualizado();
    }

    @Test
    void shouldKeepFormOpenWhenInitialBalanceIsNegative() throws Exception {
        DadosContaForm dados = new DadosContaForm(
                "Conta teste",
                TipoConta.OUTRO,
                null,
                new BigDecimal("-5.00")
        );
        doAnswer(invocation -> {
            Consumer<DadosContaForm> consumer = invocation.getArgument(0);
            consumer.accept(dados);
            return null;
        }).when(contaView).abrirFormularioCadastro(any());
        when(contaService.cadastrar(1L, "Conta teste", TipoConta.OUTRO, null, new BigDecimal("-5.00")))
                .thenThrow(new ValidacaoException("Saldo inicial nao pode ser negativo."));

        contaController.novaConta();

        verify(contaView).exibirErroFormulario("Saldo inicial nao pode ser negativo.");
        verify(contaView, never()).fecharFormulario();
        verify(contaView).exibirCarregamento(false);
    }

    @Test
    void shouldEditAccount() throws Exception {
        Conta conta = conta(21L, "Banco", TipoConta.CONTA_CORRENTE, true);
        DadosContaForm dados = new DadosContaForm(
                "Banco principal",
                TipoConta.CONTA_CORRENTE,
                "Banco Azul",
                new BigDecimal("500.00")
        );
        doAnswer(invocation -> {
            Consumer<DadosContaForm> consumer = invocation.getArgument(1);
            consumer.accept(dados);
            return null;
        }).when(contaView).abrirFormularioEdicao(any(), any());
        when(contaService.atualizar(
                21L,
                1L,
                "Banco principal",
                TipoConta.CONTA_CORRENTE,
                "Banco Azul",
                new BigDecimal("500.00")
        )).thenReturn(conta);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(contaService.consultarSaldoAtual(21L, 1L)).thenReturn(new BigDecimal("500.00"));

        contaController.editar(conta);

        verify(contaService).atualizar(
                21L,
                1L,
                "Banco principal",
                TipoConta.CONTA_CORRENTE,
                "Banco Azul",
                new BigDecimal("500.00")
        );
        verify(contaView).fecharFormulario();
        verify(contaView).exibirMensagemSucesso("Conta atualizada com sucesso.");
    }

    @Test
    void shouldKeepFormOpenWhenAccountEditionFails() throws Exception {
        Conta conta = conta(99L, "Fantasma", TipoConta.OUTRO, true);
        DadosContaForm dados = new DadosContaForm("Fantasma", TipoConta.OUTRO, null, new BigDecimal("0.00"));
        doAnswer(invocation -> {
            Consumer<DadosContaForm> consumer = invocation.getArgument(1);
            consumer.accept(dados);
            return null;
        }).when(contaView).abrirFormularioEdicao(any(), any());
        when(contaService.atualizar(99L, 1L, "Fantasma", TipoConta.OUTRO, null, new BigDecimal("0.00")))
                .thenThrow(new RegraNegocioException("Conta nao encontrada."));

        contaController.editar(conta);

        verify(contaView).exibirErroFormulario("Conta nao encontrada.");
        verify(contaView, never()).fecharFormulario();
    }

    @Test
    void shouldInactivateAccount() throws Exception {
        Conta conta = conta(30L, "Reserva", TipoConta.POUPANCA, true);
        when(contaView.confirmarAlteracaoStatus(conta, false)).thenReturn(true);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(contaService.consultarSaldoAtual(30L, 1L)).thenReturn(new BigDecimal("800.00"));

        contaController.alterarStatus(conta);

        verify(contaService).alterarStatus(30L, 1L, false);
        verify(contaView).exibirMensagemSucesso("Conta inativada com sucesso.");
    }

    @Test
    void shouldActivateAccount() throws Exception {
        Conta conta = conta(30L, "Reserva", TipoConta.POUPANCA, false);
        when(contaView.confirmarAlteracaoStatus(conta, true)).thenReturn(true);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of(conta));
        when(contaService.consultarSaldoAtual(30L, 1L)).thenReturn(new BigDecimal("800.00"));

        contaController.alterarStatus(conta);

        verify(contaService).alterarStatus(30L, 1L, true);
        verify(contaView).exibirMensagemSucesso("Conta ativada com sucesso.");
    }

    @Test
    void shouldDeleteAccount() throws Exception {
        Conta conta = conta(40L, "Conta antiga", TipoConta.OUTRO, true);
        when(contaView.confirmarExclusao(conta)).thenReturn(true);
        when(contaService.listarPorUsuario(1L)).thenReturn(List.of());

        contaController.excluir(conta);

        verify(contaService).excluir(40L, 1L);
        verify(contaView).exibirMensagemSucesso("Conta excluida com sucesso.");
    }

    @Test
    void shouldShowBlockedDeleteMessageWhenAccountHasLinkedTransactions() throws Exception {
        Conta conta = conta(40L, "Conta antiga", TipoConta.OUTRO, true);
        when(contaView.confirmarExclusao(conta)).thenReturn(true);
        doThrow(
                new RegraNegocioException("A conta nao pode ser excluida porque possui transacoes vinculadas.")
        ).when(contaService).excluir(40L, 1L);

        contaController.excluir(conta);

        verify(contaView).exibirMensagemErro(
                "A conta nao pode ser excluida porque possui transacoes vinculadas. Voce pode inativa-la."
        );
    }

    @Test
    void shouldShowBusinessMessageWhenAccountDoesNotExist() throws Exception {
        Conta conta = conta(99L, "Fantasma", TipoConta.OUTRO, true);
        DadosContaForm dados = new DadosContaForm("Fantasma", TipoConta.OUTRO, null, new BigDecimal("0.00"));
        doAnswer(invocation -> {
            Consumer<DadosContaForm> consumer = invocation.getArgument(1);
            consumer.accept(dados);
            return null;
        }).when(contaView).abrirFormularioEdicao(any(), any());
        when(contaService.atualizar(99L, 1L, "Fantasma", TipoConta.OUTRO, null, new BigDecimal("0.00")))
                .thenThrow(new RegraNegocioException("Conta nao encontrada."));

        contaController.editar(conta);

        verify(contaView).exibirErroFormulario("Conta nao encontrada.");
    }

    @Test
    void shouldShowTechnicalErrorWhenLoadingAccountsFails() throws Exception {
        when(contaService.listarPorUsuario(1L)).thenThrow(new SQLException("db off"));

        contaController.carregar();

        verify(contaView).exibirMensagemErro("Nao foi possivel acessar as contas. Tente novamente.");
        verify(contaView).exibirCarregamento(false);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Raissa");
        usuario.setEmail("raissa@example.com");
        usuario.setAtivo(true);
        return usuario;
    }

    private Conta conta(Long id, String nome, TipoConta tipo, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setUsuarioId(1L);
        conta.setNome(nome);
        conta.setTipo(tipo);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }
}
