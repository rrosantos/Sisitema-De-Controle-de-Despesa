package br.com.controledespesas.app;

import br.com.controledespesas.controller.AsyncTaskExecutor;
import br.com.controledespesas.controller.SwingWorkerAsyncTaskExecutor;
import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.DashboardDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.session.SessaoUsuario;

public class ApplicationContext {

    private final ConnectionProvider connectionProvider;
    private final UsuarioDAO usuarioDAO;
    private final CategoriaDAO categoriaDAO;
    private final ContaDAO contaDAO;
    private final TransacaoDAO transacaoDAO;
    private final DashboardDAO dashboardDAO;
    private final CofrinhoDAO cofrinhoDAO;
    private final MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;
    private final PasswordHasher passwordHasher;
    private final SessaoUsuario sessaoUsuario;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public ApplicationContext() {
        this.connectionProvider = DatabaseConnection::getConnection;
        this.usuarioDAO = new UsuarioDAO();
        this.categoriaDAO = new CategoriaDAO();
        this.contaDAO = new ContaDAO();
        this.transacaoDAO = new TransacaoDAO();
        this.dashboardDAO = new DashboardDAO(connectionProvider);
        this.cofrinhoDAO = new CofrinhoDAO();
        this.movimentacaoCofrinhoDAO = new MovimentacaoCofrinhoDAO();
        this.passwordHasher = new PasswordHasher();
        this.sessaoUsuario = new SessaoUsuario();
        this.asyncTaskExecutor = new SwingWorkerAsyncTaskExecutor();
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public SessaoUsuario getSessaoUsuario() {
        return sessaoUsuario;
    }

    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }

    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public CategoriaDAO getCategoriaDAO() {
        return categoriaDAO;
    }

    public ContaDAO getContaDAO() {
        return contaDAO;
    }

    public TransacaoDAO getTransacaoDAO() {
        return transacaoDAO;
    }

    public CofrinhoDAO getCofrinhoDAO() {
        return cofrinhoDAO;
    }

    public DashboardDAO getDashboardDAO() {
        return dashboardDAO;
    }

    public MovimentacaoCofrinhoDAO getMovimentacaoCofrinhoDAO() {
        return movimentacaoCofrinhoDAO;
    }
}
