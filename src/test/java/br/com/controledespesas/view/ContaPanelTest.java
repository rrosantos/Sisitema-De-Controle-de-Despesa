package br.com.controledespesas.view;

import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContaPanelTest {

    @Test
    void shouldShowOrderingComboWithExpectedOptionsAndDefaultValue() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> ordenacao = findByName(panel, "ordenacaoContaComboBox", JComboBox.class);

            assertEquals("Ordenar por", findLabelByText(panel, "Ordenar por").getText());
            assertEquals(4, ordenacao.getItemCount());
            assertEquals(OrdenacaoConta.NOME_CRESCENTE, ordenacao.getItemAt(0));
            assertEquals(OrdenacaoConta.NOME_DECRESCENTE, ordenacao.getItemAt(1));
            assertEquals(OrdenacaoConta.MAIOR_SALDO, ordenacao.getItemAt(2));
            assertEquals(OrdenacaoConta.MENOR_SALDO, ordenacao.getItemAt(3));
            assertEquals(OrdenacaoConta.NOME_CRESCENTE, ordenacao.getSelectedItem());
        });
    }

    @Test
    void shouldFilterAccountsBySelectedInstitutionField() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> campoPesquisa = findByName(panel, "campoPesquisaContaComboBox", JComboBox.class);
            JTextField termo = findByName(panel, "termoPesquisaContaField", JTextField.class);
            JButton filtrar = findByName(panel, "filtrarContasButton", JButton.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            campoPesquisa.setSelectedItem(CampoPesquisaConta.INSTITUICAO);
            termo.setText(" nubank ");
            filtrar.doClick();

            assertEquals(1, tabela.getRowCount());
            assertEquals("Conta digital", tabela.getValueAt(0, 0));
        });
    }

    @Test
    void shouldFilterAccountsByNameAndKeepTypeAndStatusFiltersTogether() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> tipo = findByName(panel, "filtroTipoContaComboBox", JComboBox.class);
            JComboBox<?> status = findByName(panel, "filtroStatusContaComboBox", JComboBox.class);
            JTextField termo = findByName(panel, "termoPesquisaContaField", JTextField.class);
            JButton filtrar = findByName(panel, "filtrarContasButton", JButton.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            tipo.setSelectedItem("Carteira");
            status.setSelectedItem("Ativas");
            termo.setText("CARTEIRA");
            filtrar.doClick();

            assertEquals(1, tabela.getRowCount());
            assertEquals("Carteira principal", tabela.getValueAt(0, 0));
        });
    }

    @Test
    void shouldApplySelectedOrderingWithFilterButton() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> ordenacao = findByName(panel, "ordenacaoContaComboBox", JComboBox.class);
            JButton filtrar = findByName(panel, "filtrarContasButton", JButton.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            ordenacao.setSelectedItem(OrdenacaoConta.MAIOR_SALDO);
            filtrar.doClick();

            assertEquals("Conta digital", tabela.getValueAt(0, 0));
            assertEquals("Carteira principal", tabela.getValueAt(1, 0));
            assertEquals("Banco antigo", tabela.getValueAt(2, 0));
            assertEquals("Minha carteira", tabela.getValueAt(3, 0));
        });
    }

    @Test
    void shouldReorderImmediatelyWhenOnlyOrderingChangesUsingLoadedBalances() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> ordenacao = findByName(panel, "ordenacaoContaComboBox", JComboBox.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            ordenacao.setSelectedItem(OrdenacaoConta.MENOR_SALDO);

            assertEquals("Banco antigo", tabela.getValueAt(0, 0));
            assertEquals("Carteira principal", tabela.getValueAt(1, 0));
            assertEquals("Conta digital", tabela.getValueAt(2, 0));
            assertEquals("Minha carteira", tabela.getValueAt(3, 0));
        });
    }

    @Test
    void shouldKeepFiltersWhenOrderingChanges() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> tipo = findByName(panel, "filtroTipoContaComboBox", JComboBox.class);
            JComboBox<?> status = findByName(panel, "filtroStatusContaComboBox", JComboBox.class);
            JComboBox<?> ordenacao = findByName(panel, "ordenacaoContaComboBox", JComboBox.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            tipo.setSelectedItem("Carteira");
            status.setSelectedItem("Ativas");
            ordenacao.setSelectedItem(OrdenacaoConta.NOME_DECRESCENTE);

            assertEquals("Carteira", tipo.getSelectedItem());
            assertEquals("Ativas", status.getSelectedItem());
            assertEquals(1, tabela.getRowCount());
            assertEquals("Carteira principal", tabela.getValueAt(0, 0));
        });
    }

    @Test
    void shouldClearFiltersWithoutReloadingData() throws Exception {
        runOnEdt(() -> {
            ContaPanel panel = criarPanelComContas();
            JComboBox<?> tipo = findByName(panel, "filtroTipoContaComboBox", JComboBox.class);
            JComboBox<?> status = findByName(panel, "filtroStatusContaComboBox", JComboBox.class);
            JComboBox<?> campoPesquisa = findByName(panel, "campoPesquisaContaComboBox", JComboBox.class);
            JComboBox<?> ordenacao = findByName(panel, "ordenacaoContaComboBox", JComboBox.class);
            JTextField termo = findByName(panel, "termoPesquisaContaField", JTextField.class);
            JButton limparFiltros = findByName(panel, "limparFiltrosContasButton", JButton.class);
            JTable tabela = findByName(panel, "contasTable", JTable.class);

            tipo.setSelectedItem("Conta digital");
            status.setSelectedItem("Ativas");
            campoPesquisa.setSelectedItem(CampoPesquisaConta.INSTITUICAO);
            ordenacao.setSelectedItem(OrdenacaoConta.MAIOR_SALDO);
            termo.setText("nubank");

            limparFiltros.doClick();

            assertEquals("Todos", tipo.getSelectedItem());
            assertEquals("Todas", status.getSelectedItem());
            assertEquals(CampoPesquisaConta.NOME, campoPesquisa.getSelectedItem());
            assertEquals(OrdenacaoConta.NOME_CRESCENTE, ordenacao.getSelectedItem());
            assertEquals("", termo.getText());
            assertEquals(4, tabela.getRowCount());
            assertEquals("Banco antigo", tabela.getValueAt(0, 0));
        });
    }

    private ContaPanel criarPanelComContas() {
        ContaPanel panel = new ContaPanel();
        panel.exibirContas(List.of(
                conta(1L, "Carteira principal", TipoConta.CARTEIRA, null, true),
                conta(2L, "Minha carteira", TipoConta.CARTEIRA, null, false),
                conta(3L, "Conta digital", TipoConta.CONTA_DIGITAL, "Nubank", true),
                conta(4L, "Banco antigo", TipoConta.CONTA_CORRENTE, null, true)
        ));
        panel.exibirSaldos(Map.of(
                1L, new BigDecimal("100.00"),
                3L, new BigDecimal("5000.00"),
                4L, new BigDecimal("-50.00")
        ));
        return panel;
    }

    private Conta conta(Long id, String nome, TipoConta tipo, String instituicao, boolean ativo) {
        Conta conta = new Conta();
        conta.setId(id);
        conta.setNome(nome);
        conta.setTipo(tipo);
        conta.setInstituicao(instituicao);
        conta.setSaldoInicial(BigDecimal.ZERO.setScale(2));
        conta.setAtivo(ativo);
        return conta;
    }

    private void runOnEdt(Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        SwingUtilities.invokeAndWait(runnable);
    }

    private <T extends Component> T findByName(Container root, String name, Class<T> type) {
        for (Component component : collectComponents(root)) {
            if (type.isInstance(component) && name.equals(component.getName())) {
                return type.cast(component);
            }
        }
        throw new AssertionError("Componente nao encontrado: " + name);
    }

    private JLabel findLabelByText(Container root, String text) {
        for (Component component : collectComponents(root)) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
        }
        throw new AssertionError("Label nao encontrado: " + text);
    }

    private List<Component> collectComponents(Container root) {
        List<Component> components = new ArrayList<>();
        collectComponents(root, components);
        return components;
    }

    private void collectComponents(Component component, List<Component> components) {
        components.add(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponents(child, components);
            }
        }
    }
}
