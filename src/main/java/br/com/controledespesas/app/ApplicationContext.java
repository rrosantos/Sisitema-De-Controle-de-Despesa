package br.com.controledespesas.app;

import br.com.controledespesas.controller.AsyncTaskExecutor;
import br.com.controledespesas.controller.SwingWorkerAsyncTaskExecutor;
import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.dao.TransacaoDAO;
import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.service.ContaService;
import br.com.controledespesas.service.TransacaoService;
import br.com.controledespesas.service.UsuarioService;
import br.com.controledespesas.session.SessaoUsuario;

public class ApplicationContext {

    private final ConnectionProvider connectionProvider;
    private final UsuarioDAO usuarioDAO;
    private final CategoriaDAO categoriaDAO;
    private final ContaDAO contaDAO;
    private final TransacaoDAO transacaoDAO;
    private final PasswordHasher passwordHasher;
    private final SessaoUsuario sessaoUsuario;
    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;
    private final CategoriaService categoriaService;
    private final ContaService contaService;
    private final TransacaoService transacaoService;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public ApplicationContext() {
        this.connectionProvider = DatabaseConnection::getConnection;
        this.usuarioDAO = new UsuarioDAO();
        this.categoriaDAO = new CategoriaDAO();
        this.contaDAO = new ContaDAO();
        this.transacaoDAO = new TransacaoDAO();
        this.passwordHasher = new PasswordHasher();
        this.sessaoUsuario = new SessaoUsuario();
        this.usuarioService = new UsuarioService(usuarioDAO, passwordHasher);
        this.autenticacaoService = new AutenticacaoService(usuarioDAO, passwordHasher, sessaoUsuario);
        this.categoriaService = new CategoriaService(categoriaDAO);
        this.contaService = new ContaService(contaDAO);
        this.transacaoService = new TransacaoService(transacaoDAO, categoriaDAO, contaDAO, connectionProvider);
        this.asyncTaskExecutor = new SwingWorkerAsyncTaskExecutor();
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public UsuarioService getUsuarioService() {
        return usuarioService;
    }

    public AutenticacaoService getAutenticacaoService() {
        return autenticacaoService;
    }

    public CategoriaService getCategoriaService() {
        return categoriaService;
    }

    public ContaService getContaService() {
        return contaService;
    }

    public TransacaoService getTransacaoService() {
        return transacaoService;
    }

    public SessaoUsuario getSessaoUsuario() {
        return sessaoUsuario;
    }

    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
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
}
