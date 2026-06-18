package br.com.controledespesas.view;

import br.com.controledespesas.model.Usuario;
import br.com.controledespesas.view.contract.CadastroUsuarioView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Monta e atualiza a tela Swing do modulo de CadastroUsuario.
 */
public class CadastroUsuarioPanel extends JPanel implements CadastroUsuarioView {

    private final JTextField nomeField;
    private final JTextField emailField;
    private final JPasswordField senhaField;
    private final JPasswordField confirmacaoSenhaField;
    private final JButton cadastrarButton;
    private final JButton voltarButton;
    private final JLabel mensagemLabel;
    private final String tituloTexto;
    private final String subtituloTexto;
    private final boolean exibirListaUsuarios;
    private final JTextField pesquisaUsuarioField;
    private final UsuarioTableModel usuarioTableModel;
    private final List<Usuario> usuariosOriginais = new ArrayList<>();

    private Runnable cadastrarAction;
    private Runnable voltarAction;

    public CadastroUsuarioPanel() {
        this("Criar conta", "Cadastre-se para comecar a organizar suas despesas.", "Voltar para o login", false);
    }

    public CadastroUsuarioPanel(String tituloTexto, String subtituloTexto, String voltarTexto) {
        this(tituloTexto, subtituloTexto, voltarTexto, true);
    }

    public CadastroUsuarioPanel(String tituloTexto, String subtituloTexto, String voltarTexto,
                                boolean exibirListaUsuarios) {
        this.tituloTexto = tituloTexto;
        this.subtituloTexto = subtituloTexto;
        this.exibirListaUsuarios = exibirListaUsuarios;

        setLayout(new BorderLayout());
        setBackground(UiStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        pesquisaUsuarioField = new JTextField();
        usuarioTableModel = new UsuarioTableModel();
        nomeField = new JTextField();
        emailField = new JTextField();
        senhaField = new JPasswordField();
        confirmacaoSenhaField = new JPasswordField();
        cadastrarButton = new JButton("Cadastrar");
        voltarButton = new JButton(voltarTexto);
        mensagemLabel = UiStyles.createMessageLabel();

        UiStyles.styleTextComponent(nomeField);
        UiStyles.styleTextComponent(emailField);
        UiStyles.styleTextComponent(pesquisaUsuarioField);
        UiStyles.styleTextComponent(senhaField);
        UiStyles.styleTextComponent(confirmacaoSenhaField);
        InputFormatters.instalarFiltroSemEspacos(emailField);
        UiStyles.stylePrimaryButton(cadastrarButton);
        UiStyles.styleSecondaryButton(voltarButton);

        add(criarConteudo(), BorderLayout.CENTER);
        configurarEventos();
    }

    @Override
    public String getNome() {
        return nomeField.getText();
    }

    @Override
    public String getEmail() {
        return emailField.getText();
    }

    @Override
    public char[] getSenha() {
        return senhaField.getPassword();
    }

    @Override
    public char[] getConfirmacaoSenha() {
        return confirmacaoSenhaField.getPassword();
    }

    @Override
    public void limparCampos() {
        nomeField.setText("");
        emailField.setText("");
        senhaField.setText("");
        confirmacaoSenhaField.setText("");
        limparMensagem();
    }

    @Override
    public void limparSenhas() {
        senhaField.setText("");
        confirmacaoSenhaField.setText("");
    }

    @Override
    public void setCarregando(boolean carregando) {
        nomeField.setEnabled(!carregando);
        emailField.setEnabled(!carregando);
        pesquisaUsuarioField.setEnabled(!carregando);
        senhaField.setEnabled(!carregando);
        confirmacaoSenhaField.setEnabled(!carregando);
        cadastrarButton.setEnabled(!carregando);
        voltarButton.setEnabled(!carregando);
        cadastrarButton.setText(carregando ? "Cadastrando..." : "Cadastrar");
    }

    @Override
    public void focarNome() {
        SwingUtilities.invokeLater(() -> nomeField.requestFocusInWindow());
    }

    @Override
    public void mostrarErro(String mensagem) {
        mensagemLabel.setForeground(UiStyles.ERROR);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void mostrarSucesso(String mensagem) {
        mensagemLabel.setForeground(UiStyles.SUCCESS);
        mensagemLabel.setText(mensagem != null && !mensagem.isBlank() ? mensagem : " ");
    }

    @Override
    public void limparMensagem() {
        mensagemLabel.setText(" ");
    }

    @Override
    public void setCadastrarAction(Runnable action) {
        this.cadastrarAction = action;
    }

    @Override
    public void setVoltarAction(Runnable action) {
        this.voltarAction = action;
    }

    @Override
    public boolean suportaListagemUsuarios() {
        return exibirListaUsuarios;
    }

    @Override
    public void exibirUsuarios(List<Usuario> usuarios) {
        usuariosOriginais.clear();
        if (usuarios != null) {
            usuariosOriginais.addAll(usuarios);
        }
        aplicarPesquisaUsuarios();
    }

    JButton getPrimaryButton() {
        return cadastrarButton;
    }

    private JPanel criarConteudo() {
        if (!exibirListaUsuarios) {
            return criarCard();
        }

        JPanel painel = new JPanel(new GridBagLayout());
        painel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.insets = new Insets(0, 0, 0, 18);

        constraints.gridx = 0;
        constraints.weightx = 0;
        painel.add(criarCard(), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        painel.add(criarListaUsuarios(), constraints);
        return painel;
    }

    private JPanel criarCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiStyles.WHITE);
        card.setBorder(UiStyles.createCardBorder());
        card.setPreferredSize(new Dimension(430, 470));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 14, 0);

        JLabel titulo = new JLabel(tituloTexto);
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel(subtituloTexto);
        subtitulo.setFont(UiStyles.SUBTITLE_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        card.add(titulo, constraints);
        constraints.gridy++;
        card.add(subtitulo, constraints);

        constraints.insets = new Insets(12, 0, 6, 0);
        constraints.gridy++;
        card.add(criarCampo("Nome", nomeField), constraints);

        constraints.gridy++;
        card.add(criarCampo("E-mail", emailField), constraints);

        constraints.gridy++;
        card.add(criarCampo("Senha", senhaField), constraints);

        constraints.gridy++;
        card.add(criarCampo("Confirmacao de senha", confirmacaoSenhaField), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(10, 0, 0, 0);
        card.add(mensagemLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(cadastrarButton, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 0, 0);
        card.add(voltarButton, constraints);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel criarListaUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UiStyles.WHITE);
        panel.setBorder(UiStyles.createCardBorder());
        panel.setPreferredSize(new Dimension(460, 470));

        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Pesquisa de usuarios");
        titulo.setFont(UiStyles.TITLE_FONT);
        titulo.setForeground(UiStyles.TEXT_PRIMARY);

        JLabel subtitulo = new JLabel("Busque por nome ou e-mail para consultar usuarios cadastrados.");
        subtitulo.setFont(UiStyles.SMALL_FONT);
        subtitulo.setForeground(UiStyles.TEXT_SECONDARY);

        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(6));
        cabecalho.add(subtitulo);
        cabecalho.add(Box.createVerticalStrut(14));
        cabecalho.add(criarCampo("Pesquisar", pesquisaUsuarioField));

        JTable tabela = new JTable(usuarioTableModel);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(36);
        UiStyles.styleTable(tabela);

        JScrollPane scrollPane = new JScrollPane(tabela);
        UiStyles.styleTableScrollPane(scrollPane);

        panel.add(cabecalho, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel criarCampo(String label, JTextField field) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel jLabel = new JLabel(label);
        jLabel.setFont(UiStyles.LABEL_FONT);
        jLabel.setForeground(UiStyles.TEXT_PRIMARY);

        panel.add(jLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        return panel;
    }

    private void configurarEventos() {
        cadastrarButton.addActionListener(event -> executarCadastro());
        voltarButton.addActionListener(event -> executarVoltar());
        confirmacaoSenhaField.addActionListener(event -> executarCadastro());
        pesquisaUsuarioField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                aplicarPesquisaUsuarios();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                aplicarPesquisaUsuarios();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                aplicarPesquisaUsuarios();
            }
        });
    }

    private void executarCadastro() {
        if (cadastrarAction != null) {
            cadastrarAction.run();
        }
    }

    private void executarVoltar() {
        if (voltarAction != null) {
            voltarAction.run();
        }
    }

    private void aplicarPesquisaUsuarios() {
        String termo = pesquisaUsuarioField.getText();
        String termoNormalizado = termo != null ? termo.trim().toLowerCase(Locale.ROOT) : "";
        List<Usuario> filtrados = usuariosOriginais.stream()
                .filter(usuario -> correspondePesquisa(usuario, termoNormalizado))
                .toList();
        usuarioTableModel.atualizarUsuarios(filtrados);
    }

    private boolean correspondePesquisa(Usuario usuario, String termo) {
        if (termo == null || termo.isBlank()) {
            return true;
        }
        if (usuario == null) {
            return false;
        }
        String nome = usuario.getNome() != null ? usuario.getNome().toLowerCase(Locale.ROOT) : "";
        String email = usuario.getEmail() != null ? usuario.getEmail().toLowerCase(Locale.ROOT) : "";
        return nome.contains(termo) || email.contains(termo);
    }

    private static final class UsuarioTableModel extends AbstractTableModel {

        private static final String[] COLUNAS = {"ID", "Nome", "E-mail", "Status"};
        private final List<Usuario> usuarios = new ArrayList<>();

        void atualizarUsuarios(List<Usuario> novosUsuarios) {
            usuarios.clear();
            if (novosUsuarios != null) {
                usuarios.addAll(novosUsuarios);
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return usuarios.size();
        }

        @Override
        public int getColumnCount() {
            return COLUNAS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUNAS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Usuario usuario = usuarios.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> usuario.getId();
                case 1 -> usuario.getNome();
                case 2 -> usuario.getEmail();
                case 3 -> usuario.isAtivo() ? "Ativo" : "Inativo";
                default -> "";
            };
        }
    }
}
