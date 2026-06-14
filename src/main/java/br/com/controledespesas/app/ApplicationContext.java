package br.com.controledespesas.app;

import br.com.controledespesas.controller.AsyncTaskExecutor;
import br.com.controledespesas.controller.SwingWorkerAsyncTaskExecutor;
import br.com.controledespesas.dao.UsuarioDAO;
import br.com.controledespesas.database.ConnectionProvider;
import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.security.PasswordHasher;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.service.UsuarioService;
import br.com.controledespesas.session.SessaoUsuario;

public class ApplicationContext {

    private final ConnectionProvider connectionProvider;
    private final UsuarioDAO usuarioDAO;
    private final PasswordHasher passwordHasher;
    private final SessaoUsuario sessaoUsuario;
    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public ApplicationContext() {
        this.connectionProvider = DatabaseConnection::getConnection;
        this.usuarioDAO = new UsuarioDAO();
        this.passwordHasher = new PasswordHasher();
        this.sessaoUsuario = new SessaoUsuario();
        this.usuarioService = new UsuarioService(usuarioDAO, passwordHasher);
        this.autenticacaoService = new AutenticacaoService(usuarioDAO, passwordHasher, sessaoUsuario);
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

    public SessaoUsuario getSessaoUsuario() {
        return sessaoUsuario;
    }

    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }
}
