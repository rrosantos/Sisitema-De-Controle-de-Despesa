package br.com.controledespesas.service;

import br.com.controledespesas.dao.DashboardDAO;
import br.com.controledespesas.dto.DashboardResumo;
import br.com.controledespesas.dto.ResumoCategoriaDashboard;
import br.com.controledespesas.dto.ResumoCofrinhoDashboard;
import br.com.controledespesas.dto.ResumoContaDashboard;
import br.com.controledespesas.dto.TransacaoRecenteDashboard;
import br.com.controledespesas.util.CofrinhoProgressCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DashboardService {

    private static final int LIMITE_CATEGORIAS = 5;
    private static final int LIMITE_TRANSACOES_RECENTES = 5;
    private static final int LIMITE_COFRINHOS = 4;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        this(new DashboardDAO());
    }

    public DashboardService(DashboardDAO dashboardDAO) {
        this.dashboardDAO = Objects.requireNonNull(dashboardDAO, "dashboardDAO nao pode ser nulo.");
    }

    public DashboardResumo carregar(Long usuarioId, LocalDate dataInicial, LocalDate dataFinal) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        ServiceValidationUtils.validateDateRange(dataInicial, dataFinal);

        List<ResumoContaDashboard> contas = normalizarLista(dashboardDAO.listarSaldosPorConta(idUsuario));
        BigDecimal saldoTotal = calcularSaldoTotal(contas);
        int contasAtivas = contarContasAtivas(contas);

        BigDecimal totalReceitas = normalizarValor(dashboardDAO.calcularReceitasPeriodo(idUsuario, dataInicial, dataFinal));
        BigDecimal totalDespesas = normalizarValor(dashboardDAO.calcularDespesasPeriodo(idUsuario, dataInicial, dataFinal));
        BigDecimal resultadoPeriodo = totalReceitas.subtract(totalDespesas);

        List<ResumoCategoriaDashboard> categorias = calcularPercentuaisCategorias(
                normalizarLista(dashboardDAO.listarDespesasPorCategoria(idUsuario, dataInicial, dataFinal, LIMITE_CATEGORIAS)),
                totalDespesas
        );
        List<TransacaoRecenteDashboard> transacoesRecentes = normalizarLista(
                dashboardDAO.listarTransacoesRecentes(idUsuario, dataInicial, dataFinal, LIMITE_TRANSACOES_RECENTES)
        );
        List<ResumoCofrinhoDashboard> cofrinhos = calcularPercentuaisCofrinhos(
                normalizarLista(dashboardDAO.listarResumoCofrinhos(idUsuario, LIMITE_COFRINHOS))
        );
        int transacoesPendentes = dashboardDAO.contarTransacoesPendentes(idUsuario);

        return new DashboardResumo(
                dataInicial,
                dataFinal,
                saldoTotal,
                totalReceitas,
                totalDespesas,
                resultadoPeriodo,
                contasAtivas,
                transacoesPendentes,
                contas,
                categorias,
                transacoesRecentes,
                cofrinhos
        );
    }

    private BigDecimal calcularSaldoTotal(List<ResumoContaDashboard> contas) {
        BigDecimal saldoTotal = BigDecimal.ZERO;
        for (ResumoContaDashboard conta : contas) {
            saldoTotal = saldoTotal.add(normalizarValor(conta.saldoAtual()));
        }
        return saldoTotal;
    }

    private int contarContasAtivas(List<ResumoContaDashboard> contas) {
        int total = 0;
        for (ResumoContaDashboard conta : contas) {
            if (conta.ativa()) {
                total++;
            }
        }
        return total;
    }

    private List<ResumoCategoriaDashboard> calcularPercentuaisCategorias(List<ResumoCategoriaDashboard> categorias,
                                                                         BigDecimal totalDespesas) {
        List<ResumoCategoriaDashboard> resultado = new ArrayList<>();
        BigDecimal denominador = normalizarValor(totalDespesas);

        for (ResumoCategoriaDashboard categoria : categorias) {
            BigDecimal percentual = BigDecimal.ZERO;
            if (denominador.compareTo(BigDecimal.ZERO) > 0) {
                percentual = normalizarValor(categoria.total())
                        .multiply(CEM)
                        .divide(denominador, 2, RoundingMode.HALF_UP);
            }
            resultado.add(new ResumoCategoriaDashboard(
                    categoria.categoriaId(),
                    categoria.nomeCategoria(),
                    categoria.total(),
                    percentual
            ));
        }

        return resultado;
    }

    private List<ResumoCofrinhoDashboard> calcularPercentuaisCofrinhos(List<ResumoCofrinhoDashboard> cofrinhos) {
        List<ResumoCofrinhoDashboard> resultado = new ArrayList<>();

        for (ResumoCofrinhoDashboard cofrinho : cofrinhos) {
            BigDecimal percentual = BigDecimal.ZERO;
            if (cofrinho.valorMeta() != null && cofrinho.valorMeta().compareTo(BigDecimal.ZERO) > 0) {
                percentual = CofrinhoProgressCalculator.calculateProgressPercentage(
                        normalizarValor(cofrinho.valorAtual()),
                        cofrinho.valorMeta()
                );
            }

            resultado.add(new ResumoCofrinhoDashboard(
                    cofrinho.cofrinhoId(),
                    cofrinho.nome(),
                    cofrinho.status(),
                    cofrinho.valorAtual(),
                    cofrinho.valorMeta(),
                    percentual,
                    cofrinho.dataLimite()
            ));
        }

        return resultado;
    }

    private BigDecimal normalizarValor(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private <T> List<T> normalizarLista(List<T> lista) {
        return lista != null ? List.copyOf(lista) : List.of();
    }
}
