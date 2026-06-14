package br.com.controledespesas.app;

import br.com.controledespesas.database.DatabaseConnection;
import br.com.controledespesas.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {

    public static void main(String[] args) {
        new Main().start();
    }

    private void start() {
        boolean connectionAvailable = DatabaseConnection.testConnection();

        SwingUtilities.invokeLater(() -> {
            applySystemLookAndFeel();

            if (connectionAvailable) {
                openMainFrame();
            } else {
                showConnectionErrorDialog();
            }
        });
    }

    private void openMainFrame() {
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }

    private void showConnectionErrorDialog() {
        JOptionPane.showMessageDialog(
                null,
                "Nao foi possivel conectar ao MySQL.\nVerifique o arquivo .env, o schema inicial e o status do banco.",
                "Erro de conexao",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("System look and feel could not be applied.");
        }
    }
}
