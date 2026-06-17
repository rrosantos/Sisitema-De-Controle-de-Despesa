package br.com.controledespesas.controller;

@FunctionalInterface
public interface DashboardRefreshNotifier {

    DashboardRefreshNotifier NO_OP = () -> {
    };

    void marcarDashboardComoDesatualizado();
}
