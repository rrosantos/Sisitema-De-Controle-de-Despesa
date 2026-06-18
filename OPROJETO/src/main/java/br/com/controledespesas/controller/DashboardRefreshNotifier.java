package br.com.controledespesas.controller;

@FunctionalInterface
/**
 * Define o contrato para sinalizar que o dashboard precisa ser recarregado.
 */
public interface DashboardRefreshNotifier {

    DashboardRefreshNotifier NO_OP = () -> {
    };

    void marcarDashboardComoDesatualizado();
}
