package br.com.controledespesas.controller;

import br.com.controledespesas.exception.AutenticacaoException;
import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.MainView;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    private final AutenticacaoService autenticacaoService;
    private final SessaoUsuario sessaoUsuario;
    private final MainView mainView;
    private final ApplicationController applicationController;

    public MainController(AutenticacaoService autenticacaoService, SessaoUsuario sessaoUsuario,
                          MainView mainView, ApplicationController applicationController) {
        this.autenticacaoService = Objects.requireNonNull(autenticacaoService, "autenticacaoService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.mainView = Objects.requireNonNull(mainView, "mainView nao pode ser nulo.");
        this.applicationController =
                Objects.requireNonNull(applicationController, "applicationController nao pode ser nulo.");
    }

    public void iniciar() {
        try {
            Usuario usuario = sessaoUsuario.exigirUsuarioAutenticado();
            mainView.exibirUsuario(usuario.getNome(), usuario.getEmail());
            mainView.definirAcaoSair(this::realizarLogout);
            mainView.abrir();
        } catch (AutenticacaoException exception) {
            LOGGER.log(Level.FINE, "Tentativa de abrir tela principal sem sessao autenticada.", exception);
            mainView.fechar();
            applicationController.mostrarLogin();
        }
    }

    public void realizarLogout() {
        autenticacaoService.sair();
        mainView.fechar();
        applicationController.realizarLogout();
    }
}
