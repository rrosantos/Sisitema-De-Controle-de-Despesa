package br.com.controledespesas.service;

import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.StatusCofrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CofrinhoServiceTest {

    @Mock
    private CofrinhoDAO cofrinhoDAO;

    @Mock
    private MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    private CofrinhoService cofrinhoService;

    @BeforeEach
    void setUp() {
        cofrinhoService = new CofrinhoService(cofrinhoDAO, movimentacaoCofrinhoDAO, connectionProvider);
    }

    @Test
    void shouldRegisterValidSavingsGoalAlwaysInProgress() throws SQLException {
        when(cofrinhoDAO.inserir(any(Cofrinho.class))).thenAnswer(invocation -> {
            Cofrinho cofrinho = invocation.getArgument(0);
            cofrinho.setId(30L);
            return 30L;
        });

        Cofrinho cofrinho = cofrinhoService.cadastrar(
                1L,
                "Reserva",
                "Emergencias",
                new BigDecimal("1000.00"),
                null
        );

        assertEquals(30L, cofrinho.getId());
        assertEquals(StatusCofrinho.EM_ANDAMENTO, cofrinho.getStatus());
    }

    @Test
    void shouldRejectNonPositiveGoalValue() {
        assertThrows(ValidacaoException.class,
                () -> cofrinhoService.cadastrar(1L, "Reserva", null, BigDecimal.ZERO, null));
    }

    @Test
    void shouldRejectSavingsGoalFromAnotherUser() throws SQLException {
        when(cofrinhoDAO.buscarPorId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(RegraNegocioException.class,
                () -> cofrinhoService.buscarPorId(99L, 1L));
    }

    @Test
    void shouldConsultCurrentValueAndProgress() throws SQLException {
        when(movimentacaoCofrinhoDAO.calcularValorAtual(30L, 1L)).thenReturn(Optional.of(new BigDecimal("500.00")));
        when(movimentacaoCofrinhoDAO.calcularPercentualProgresso(30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("50.00")));

        BigDecimal valorAtual = cofrinhoService.consultarValorAtual(30L, 1L);
        BigDecimal percentual = cofrinhoService.consultarPercentualProgresso(30L, 1L);

        assertAll(
                () -> assertEquals(new BigDecimal("500.00"), valorAtual),
                () -> assertEquals(new BigDecimal("50.00"), percentual)
        );
    }

    @Test
    void shouldRecalculateStatusWhenUpdatingGoalValue() throws SQLException {
        prepararConexaoTransacional();
        Cofrinho cofrinho = cofrinho(30L, new BigDecimal("1000.00"), StatusCofrinho.EM_ANDAMENTO);
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinho));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("600.00")));

        Cofrinho atualizado = cofrinhoService.atualizar(
                30L,
                1L,
                "Reserva",
                "Emergencias",
                new BigDecimal("500.00"),
                null
        );

        assertEquals(StatusCofrinho.CONCLUIDO, atualizado.getStatus());
        verify(cofrinhoDAO).atualizar(eq(connection), argThat(candidato ->
                candidato.getStatus() == StatusCofrinho.CONCLUIDO
                        && new BigDecimal("500.00").compareTo(candidato.getValorMeta()) == 0
        ));
        verify(connection).commit();
    }

    @Test
    void shouldRecalculateStatusWhenReactivatingCanceledGoal() throws SQLException {
        prepararConexaoTransacional();
        Cofrinho cofrinho = cofrinho(30L, new BigDecimal("500.00"), StatusCofrinho.CANCELADO);
        when(cofrinhoDAO.buscarPorIdParaAtualizacao(connection, 30L, 1L)).thenReturn(Optional.of(cofrinho));
        when(movimentacaoCofrinhoDAO.calcularValorAtual(connection, 30L, 1L))
                .thenReturn(Optional.of(new BigDecimal("550.00")));

        cofrinhoService.reativar(30L, 1L);

        verify(cofrinhoDAO).atualizarStatus(connection, 30L, 1L, StatusCofrinho.CONCLUIDO);
        verify(connection).commit();
    }

    private Cofrinho cofrinho(Long id, BigDecimal valorMeta, StatusCofrinho status) {
        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setId(id);
        cofrinho.setUsuarioId(1L);
        cofrinho.setNome("Reserva");
        cofrinho.setDescricao("Emergencias");
        cofrinho.setValorMeta(valorMeta);
        cofrinho.setStatus(status);
        return cofrinho;
    }

    private void prepararConexaoTransacional() throws SQLException {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
    }
}
