package br.com.controledespesas.app;

import br.com.controledespesas.controller.ApplicationController;
import br.com.controledespesas.database.DatabaseConnection;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        new Main().launch();
    }

    private void launch() {
        if (!DatabaseConnection.testConnection()) {
            LOGGER.warning("Nao foi possivel estabelecer conexao inicial com o banco de dados.");
            SwingUtilities.invokeLater(() -> {
                applySystemLookAndFeel();
                showConnectionErrorDialog();
            });
            return;
        }

        SwingUtilities.invokeLater(this::startInterface);
    }

    private void startInterface() {
        applySystemLookAndFeel();

        try {
            ApplicationContext applicationContext = new ApplicationContext();
            ApplicationController applicationController = new ApplicationController(applicationContext);
            applicationController.iniciar();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Falha inesperada ao iniciar a aplicacao.", exception);
            JOptionPane.showMessageDialog(
                    null,
                    "Ocorreu um erro ao iniciar a aplicacao.",
                    "Erro de inicializacao",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showConnectionErrorDialog() {
        JOptionPane.showMessageDialog(
                null,
                "Nao foi possivel conectar ao banco de dados.\n"
                        + "Verifique se o MySQL esta ativo e se o arquivo .env esta configurado corretamente.",
                "Erro de conexao",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Nao foi possivel aplicar o look and feel nativo.", exception);
        }
    }
}
