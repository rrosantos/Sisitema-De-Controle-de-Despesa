package br.com.controledespesas.controller;

import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.service.CategoriaService;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.view.contract.CategoriaView;
import br.com.controledespesas.view.contract.DadosCategoriaForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CategoriaController {

    private static final Logger LOGGER = Logger.getLogger(CategoriaController.class.getName());
    private static final String MENSAGEM_ERRO_TECNICO =
            "Nao foi possivel acessar as categorias. Tente novamente.";
    private static final String MENSAGEM_EXCLUSAO_BLOQUEADA =
            "A categoria nao pode ser excluida porque possui transacoes vinculadas. Voce pode inativa-la.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Categoria cadastrada com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Categoria atualizada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Categoria excluida com sucesso.";
    private static final String MENSAGEM_ATIVACAO_SUCESSO = "Categoria ativada com sucesso.";
    private static final String MENSAGEM_INATIVACAO_SUCESSO = "Categoria inativada com sucesso.";

    private final CategoriaService categoriaService;
    private final SessaoUsuario sessaoUsuario;
    private final CategoriaView categoriaView;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public CategoriaController(CategoriaService categoriaService, SessaoUsuario sessaoUsuario,
                               CategoriaView categoriaView, AsyncTaskExecutor asyncTaskExecutor) {
        this.categoriaService = Objects.requireNonNull(categoriaService, "categoriaService nao pode ser nulo.");
        this.sessaoUsuario = Objects.requireNonNull(sessaoUsuario, "sessaoUsuario nao pode ser nulo.");
        this.categoriaView = Objects.requireNonNull(categoriaView, "categoriaView nao pode ser nulo.");
        this.asyncTaskExecutor = Objects.requireNonNull(asyncTaskExecutor, "asyncTaskExecutor nao pode ser nulo.");
        configurarAcoes();
    }

    public void carregar() {
        categoriaView.limparMensagem();
        executarOperacao(this::carregarResultadoInicial, false);
    }

    public void novaCategoria() {
        categoriaView.abrirFormularioCadastro().ifPresent(dados ->
                executarOperacao(() -> cadastrarCategoria(dados), true));
    }

    public void editar(Categoria categoria) {
        if (categoria == null) {
            return;
        }

        categoriaView.abrirFormularioEdicao(categoria).ifPresent(dados ->
                executarOperacao(() -> atualizarCategoria(categoria, dados), true));
    }

    public void alterarStatus(Categoria categoria) {
        if (categoria == null) {
            return;
        }

        boolean novoStatus = !categoria.isAtivo();
        if (!categoriaView.confirmarAlteracaoStatus(categoria, novoStatus)) {
            return;
        }

        executarOperacao(() -> alterarStatusCategoria(categoria, novoStatus), true);
    }

    public void excluir(Categoria categoria) {
        if (categoria == null || !categoriaView.confirmarExclusao(categoria)) {
            return;
        }

        executarOperacao(() -> excluirCategoria(categoria), true);
    }

    private void configurarAcoes() {
        categoriaView.definirAcaoNovaCategoria(this::novaCategoria);
        categoriaView.definirAcaoEditar(this::editar);
        categoriaView.definirAcaoAlterarStatus(this::alterarStatus);
        categoriaView.definirAcaoExcluir(this::excluir);
    }

    private void executarOperacao(Callable<CategoriaResultado> operacao, boolean limparMensagemAntes) {
        if (limparMensagemAntes) {
            categoriaView.limparMensagem();
        }

        categoriaView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                this::aplicarResultado,
                this::tratarErro,
                () -> categoriaView.exibirCarregamento(false)
        );
    }

    private CategoriaResultado carregarResultadoInicial() throws SQLException {
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), null);
    }

    private CategoriaResultado cadastrarCategoria(DadosCategoriaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        categoriaService.cadastrar(usuarioId, dados.nome(), dados.tipo(), dados.descricao());
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_CADASTRO_SUCESSO);
    }

    private CategoriaResultado atualizarCategoria(Categoria categoria, DadosCategoriaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        categoriaService.atualizar(categoria.getId(), usuarioId, dados.nome(), dados.tipo(), dados.descricao());
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_EDICAO_SUCESSO);
    }

    private CategoriaResultado alterarStatusCategoria(Categoria categoria, boolean novoStatus) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        categoriaService.alterarStatus(categoria.getId(), usuarioId, novoStatus);
        String mensagem = novoStatus ? MENSAGEM_ATIVACAO_SUCESSO : MENSAGEM_INATIVACAO_SUCESSO;
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), mensagem);
    }

    private CategoriaResultado excluirCategoria(Categoria categoria) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        categoriaService.excluir(categoria.getId(), usuarioId);
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_EXCLUSAO_SUCESSO);
    }

    private List<Categoria> listarCategoriasUsuarioAtual() throws SQLException {
        return categoriaService.listarPorUsuario(sessaoUsuario.exigirUsuarioId());
    }

    private void aplicarResultado(CategoriaResultado resultado) {
        categoriaView.exibirCategorias(resultado.categorias());
        if (resultado.categorias().isEmpty()) {
            categoriaView.exibirEstadoVazio();
        }
        if (resultado.mensagemSucesso() != null && !resultado.mensagemSucesso().isBlank()) {
            categoriaView.exibirMensagemSucesso(resultado.mensagemSucesso());
        }
    }

    private void tratarErro(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            categoriaView.exibirMensagemErro(mapearMensagemNegocio(throwable.getMessage()));
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha tecnica ao processar categorias.", throwable);
            categoriaView.exibirMensagemErro(MENSAGEM_ERRO_TECNICO);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado no modulo de categorias.", throwable);
        categoriaView.exibirMensagemErro(MENSAGEM_ERRO_TECNICO);
    }

    private String mapearMensagemNegocio(String mensagemOriginal) {
        if (mensagemOriginal != null && mensagemOriginal.contains("transacoes vinculadas")) {
            return MENSAGEM_EXCLUSAO_BLOQUEADA;
        }
        return mensagemOriginal != null && !mensagemOriginal.isBlank() ? mensagemOriginal : MENSAGEM_ERRO_TECNICO;
    }

    private record CategoriaResultado(List<Categoria> categorias, String mensagemSucesso) {
    }
}
