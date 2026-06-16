package br.com.controledespesas.controller;

import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.DashboardService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.DashboardView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DashboardView dashboardView;

    private final AtomicReference<LocalDate> dataInicial = new AtomicReference<>();
    private final AtomicReference<LocalDate> dataFinal = new AtomicReference<>();

    private SessaoUsuario sessaoUsuario;
    private DashboardController dashboardController;
    private Clock clock;

    @BeforeEach
    void setUp() {
        sessaoUsuario = new SessaoUsuario();
        sessaoUsuario.iniciar(usuario());
        clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

        lenient().doAnswer(invocation -> {
            dataInicial.set(invocation.getArgument(0));
            dataFinal.set(invocation.getArgument(1));
            return null;
        }).when(dashboardView).definirPeriodo(any(), any());
        lenient().when(dashboardView.obterDataInicial()).thenAnswer(invocation -> dataInicial.get());
        lenient().when(dashboardView.obterDataFinal()).thenAnswer(invocation -> dataFinal.get());

        dashboardController = new DashboardController(
                dashboardService,
                sessaoUsuario,
                dashboardView,
                new ImmediateAsyncTaskExecutor(),
                clock
        );
        clearInvocations(dashboardView, dashboardService);
    }

    @Test
    void shouldInitializeWithCurrentMonthPeriodAndLoadSummary() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 6, 1);
        LocalDate fim = LocalDate.of(2026, 6, 15);
        when(dashboardService.carregar(1L, inicio, fim)).thenReturn(resumo(inicio, fim));

        dashboardController.atualizarSeNecessario();

        assertEquals(inicio, dataInicial.get());
        assertEquals(fim, dataFinal.get());
        verify(dashboardView).definirPeriodo(inicio, fim);
        verify(dashboardService).carregar(1L, inicio, fim);
        verify(dashboardView).exibirCarregamento(true);
        verify(dashboardView).exibirCarregamento(false);
        verify(dashboardView).exibirResumo(any(DashboardResumo.class));
    }

    @Test
    void shouldApplyFilterUsingSessionUser() throws Exception {
        dataInicial.set(LocalDate.of(2026, 5, 1));
        dataFinal.set(LocalDate.of(2026, 5, 31));
        when(dashboardService.carregar(1L, dataInicial.get(), dataFinal.get()))
                .thenReturn(resumo(dataInicial.get(), dataFinal.get()));

        dashboardController.aplicarFiltro();

        verify(dashboardService).carregar(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
    }

    @Test
    void shouldShowValidationMessageWhenDateRangeIsInvalid() throws Exception {
        dataInicial.set(LocalDate.of(2026, 6, 20));
        dataFinal.set(LocalDate.of(2026, 6, 10));

        dashboardController.carregar();

        verify(dashboardView).exibirMensagemErro("A data inicial nao pode ser posterior a data final.");
        verify(dashboardService, never()).carregar(any(), any(), any());
    }

    @Test
    void shouldApplyShortcutPeriods() throws Exception {
        when(dashboardService.carregar(eq(1L), any(), any())).thenAnswer(invocation ->
                resumo(invocation.getArgument(1), invocation.getArgument(2))
        );

        dashboardController.mostrarMesAnterior();
        assertEquals(LocalDate.of(2026, 5, 1), dataInicial.get());
        assertEquals(LocalDate.of(2026, 5, 31), dataFinal.get());

        dashboardController.mostrarUltimosTrintaDias();
        assertEquals(LocalDate.of(2026, 5, 17), dataInicial.get());
        assertEquals(LocalDate.of(2026, 6, 15), dataFinal.get());

        dashboardController.mostrarEsteAno();
        assertEquals(LocalDate.of(2026, 1, 1), dataInicial.get());
        assertEquals(LocalDate.of(2026, 6, 15), dataFinal.get());

        dashboardController.limparPeriodo();
        assertNull(dataInicial.get());
        assertNull(dataFinal.get());
        verify(dashboardService).carregar(eq(1L), isNull(), isNull());
    }

    @Test
    void shouldShowTechnicalErrorAndRestoreLoadingState() throws Exception {
        dataInicial.set(LocalDate.of(2026, 6, 1));
        dataFinal.set(LocalDate.of(2026, 6, 15));
        when(dashboardService.carregar(1L, dataInicial.get(), dataFinal.get()))
                .thenThrow(new SQLException("db off"));

        dashboardController.carregar();

        verify(dashboardView).exibirCarregamento(true);
        verify(dashboardView).exibirCarregamento(false);
        verify(dashboardView).exibirMensagemErro("Nao foi possivel carregar o resumo financeiro. Tente novamente.");
        verify(dashboardView, never()).exibirResumo(any());
    }

    @Test
    void shouldPreservePreviousDataWhenRefreshFails() throws Exception {
        dataInicial.set(LocalDate.of(2026, 6, 1));
        dataFinal.set(LocalDate.of(2026, 6, 15));
        when(dashboardService.carregar(1L, dataInicial.get(), dataFinal.get()))
                .thenReturn(resumo(dataInicial.get(), dataFinal.get()));

        dashboardController.carregar();
        clearInvocations(dashboardView, dashboardService);

        when(dashboardService.carregar(1L, dataInicial.get(), dataFinal.get()))
                .thenThrow(new SQLException("falha"));

        dashboardController.carregar();

        verify(dashboardView, never()).exibirResumo(any());
        verify(dashboardView).exibirMensagemErro("Nao foi possivel carregar o resumo financeiro. Tente novamente.");
    }

    @Test
    void shouldReloadOnlyWhenMarkedAsStale() throws Exception {
        dataInicial.set(LocalDate.of(2026, 6, 1));
        dataFinal.set(LocalDate.of(2026, 6, 15));
        when(dashboardService.carregar(1L, dataInicial.get(), dataFinal.get()))
                .thenReturn(resumo(dataInicial.get(), dataFinal.get()));

        dashboardController.atualizarSeNecessario();
        clearInvocations(dashboardView, dashboardService);

        dashboardController.atualizarSeNecessario();
        verify(dashboardService, never()).carregar(any(), any(), any());

        dashboardController.marcarDashboardComoDesatualizado();
        dashboardController.atualizarSeNecessario();
        verify(dashboardService).carregar(1L, dataInicial.get(), dataFinal.get());
    }

    @Test
    void shouldPreventConcurrentLoads() {
        AsyncTaskExecutor asyncTaskExecutor = org.mockito.Mockito.mock(AsyncTaskExecutor.class);
        DashboardController controllerConcorrente = new DashboardController(
                dashboardService,
                sessaoUsuario,
                dashboardView,
                asyncTaskExecutor,
                clock
        );
        clearInvocations(dashboardView, dashboardService, asyncTaskExecutor);
        dataInicial.set(LocalDate.of(2026, 6, 1));
        dataFinal.set(LocalDate.of(2026, 6, 15));

        controllerConcorrente.carregar();
        controllerConcorrente.carregar();

        verify(asyncTaskExecutor, times(1)).execute(any(), any(), any(), any());
    }

    @Test
    void shouldNavigateToTransactionsAccountsAndSavingsGoals() {
        AtomicInteger totalChamadas = new AtomicInteger();

        dashboardController.definirAcaoAbrirTransacoes(totalChamadas::incrementAndGet);
        dashboardController.definirAcaoAbrirContas(totalChamadas::incrementAndGet);
        dashboardController.definirAcaoAbrirCofrinhos(totalChamadas::incrementAndGet);

        dashboardController.abrirTransacoes();
        dashboardController.abrirContas();
        dashboardController.abrirCofrinhos();

        assertEquals(3, totalChamadas.get());
    }

    private DashboardResumo resumo(LocalDate dataInicial, LocalDate dataFinal) {
        return new DashboardResumo(
                dataInicial,
                dataFinal,
                new BigDecimal("1500.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1500.00"),
                2,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of()
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
}
