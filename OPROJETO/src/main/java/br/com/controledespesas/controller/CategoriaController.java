package br.com.controledespesas.controller;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.exception.ValidacaoException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;
import br.com.controledespesas.session.SessaoUsuario;
import br.com.controledespesas.util.SqlExceptionUtils;
import br.com.controledespesas.util.ValidationUtils;
import br.com.controledespesas.view.contract.CategoriaView;
import br.com.controledespesas.view.contract.DadosCategoriaForm;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordena regras, filtros e operacoes de categorias do usuario autenticado.
 */
public class CategoriaController {

    private static final Logger LOGGER = Logger.getLogger(CategoriaController.class.getName());
    private static final int MAX_NOME = 100;
    private static final int MAX_DESCRICAO = 255;
    private static final String MENSAGEM_DUPLICIDADE = "Ja existe uma categoria com este nome e tipo.";
    private static final String MENSAGEM_ERRO_TECNICO =
            "Nao foi possivel acessar as categorias. Tente novamente.";
    private static final String MENSAGEM_ERRO_SALVAR =
            "Nao foi possivel salvar a categoria. Tente novamente.";
    private static final String MENSAGEM_EXCLUSAO_BLOQUEADA =
            "A categoria nao pode ser excluida porque possui transacoes vinculadas. Voce pode inativa-la.";
    private static final String MENSAGEM_CADASTRO_SUCESSO = "Categoria cadastrada com sucesso.";
    private static final String MENSAGEM_EDICAO_SUCESSO = "Categoria atualizada com sucesso.";
    private static final String MENSAGEM_EXCLUSAO_SUCESSO = "Categoria excluida com sucesso.";
    private static final String MENSAGEM_ATIVACAO_SUCESSO = "Categoria ativada com sucesso.";
    private static final String MENSAGEM_INATIVACAO_SUCESSO = "Categoria inativada com sucesso.";

    private final CategoriaDAO categoriaDAO;
    private final SessaoUsuario sessaoUsuario;
    private final CategoriaView categoriaView;
    private final AsyncTaskExecutor asyncTaskExecutor;

    public CategoriaController(CategoriaDAO categoriaDAO, SessaoUsuario sessaoUsuario,
                               CategoriaView categoriaView, AsyncTaskExecutor asyncTaskExecutor) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO nao pode ser nulo.");
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
        categoriaView.limparMensagem();
        categoriaView.abrirFormularioCadastro(dados -> executarOperacaoFormulario(() -> cadastrarCategoria(dados)));
    }

    public void editar(Categoria categoria) {
        if (categoria == null) {
            return;
        }

        categoriaView.limparMensagem();
        categoriaView.abrirFormularioEdicao(
                categoria,
                dados -> executarOperacaoFormulario(() -> atualizarCategoria(categoria, dados))
        );
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
                this::tratarErroPainel,
                () -> categoriaView.exibirCarregamento(false)
        );
    }

    private void executarOperacaoFormulario(Callable<CategoriaResultado> operacao) {
        categoriaView.exibirCarregamento(true);
        asyncTaskExecutor.execute(
                operacao,
                resultado -> {
                    categoriaView.fecharFormulario();
                    aplicarResultado(resultado);
                },
                this::tratarErroFormulario,
                () -> categoriaView.exibirCarregamento(false)
        );
    }

    private CategoriaResultado carregarResultadoInicial() throws SQLException {
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), null);
    }

    private CategoriaResultado cadastrarCategoria(DadosCategoriaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        cadastrar(usuarioId, dados.nome(), dados.tipo(), dados.descricao());
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_CADASTRO_SUCESSO);
    }

    private CategoriaResultado atualizarCategoria(Categoria categoria, DadosCategoriaForm dados) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        atualizar(categoria.getId(), usuarioId, dados.nome(), dados.tipo(), dados.descricao());
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_EDICAO_SUCESSO);
    }

    private CategoriaResultado alterarStatusCategoria(Categoria categoria, boolean novoStatus) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        alterarStatus(categoria.getId(), usuarioId, novoStatus);
        String mensagem = novoStatus ? MENSAGEM_ATIVACAO_SUCESSO : MENSAGEM_INATIVACAO_SUCESSO;
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), mensagem);
    }

    private CategoriaResultado excluirCategoria(Categoria categoria) throws SQLException {
        Long usuarioId = sessaoUsuario.exigirUsuarioId();
        excluir(categoria.getId(), usuarioId);
        return new CategoriaResultado(listarCategoriasUsuarioAtual(), MENSAGEM_EXCLUSAO_SUCESSO);
    }

    private List<Categoria> listarCategoriasUsuarioAtual() throws SQLException {
        return listarPorUsuario(sessaoUsuario.exigirUsuarioId());
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

    private void tratarErroPainel(Throwable throwable) {
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

    private void tratarErroFormulario(Throwable throwable) {
        if (throwable instanceof ValidacaoException || throwable instanceof RegraNegocioException) {
            categoriaView.exibirErroFormulario(mapearMensagemNegocio(throwable.getMessage()));
            return;
        }

        if (throwable instanceof SQLException) {
            LOGGER.log(Level.WARNING, "Falha tecnica ao salvar categoria.", throwable);
            categoriaView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
            return;
        }

        LOGGER.log(Level.SEVERE, "Erro inesperado ao salvar categoria.", throwable);
        categoriaView.exibirErroFormulario(MENSAGEM_ERRO_SALVAR);
    }

    private String mapearMensagemNegocio(String mensagemOriginal) {
        if (mensagemOriginal != null && mensagemOriginal.contains("transacoes vinculadas")) {
            return MENSAGEM_EXCLUSAO_BLOQUEADA;
        }
        return mensagemOriginal != null && !mensagemOriginal.isBlank() ? mensagemOriginal : MENSAGEM_ERRO_TECNICO;
    }

    private Categoria cadastrar(Long usuarioId, String nome, TipoCategoria tipo, String descricao) throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ValidationUtils.normalizeRequiredText(nome, "Nome da categoria", MAX_NOME);
        TipoCategoria tipoCategoria = ValidationUtils.requireValue(tipo, "Tipo da categoria");
        String descricaoNormalizada =
                ValidationUtils.normalizeOptionalText(descricao, "Descricao da categoria", MAX_DESCRICAO);

        if (categoriaDAO.nomeETipoExistem(idUsuario, nomeNormalizado, tipoCategoria)) {
            throw new RegraNegocioException(MENSAGEM_DUPLICIDADE);
        }

        Categoria categoria = new Categoria();
        categoria.setUsuarioId(idUsuario);
        categoria.setNome(nomeNormalizado);
        categoria.setTipo(tipoCategoria);
        categoria.setDescricao(descricaoNormalizada);
        categoria.setAtivo(true);

        try {
            categoriaDAO.inserir(categoria);
            return categoria;
        } catch (SQLException exception) {
            if (SqlExceptionUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    private List<Categoria> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        return categoriaDAO.listarPorUsuario(idUsuario);
    }

    private Categoria atualizar(Long categoriaId, Long usuarioId, String nome, TipoCategoria tipo, String descricao)
            throws SQLException {
        Long idCategoria = ValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ValidationUtils.normalizeRequiredText(nome, "Nome da categoria", MAX_NOME);
        TipoCategoria tipoCategoria = ValidationUtils.requireValue(tipo, "Tipo da categoria");
        String descricaoNormalizada =
                ValidationUtils.normalizeOptionalText(descricao, "Descricao da categoria", MAX_DESCRICAO);

        Categoria categoriaExistente = buscarCategoriaExistente(idCategoria, idUsuario);
        if (categoriaDAO.nomeETipoExistemParaOutraCategoria(idUsuario, nomeNormalizado, tipoCategoria, idCategoria)) {
            throw new RegraNegocioException(MENSAGEM_DUPLICIDADE);
        }

        if (Objects.equals(categoriaExistente.getNome(), nomeNormalizado)
                && categoriaExistente.getTipo() == tipoCategoria
                && Objects.equals(categoriaExistente.getDescricao(), descricaoNormalizada)) {
            return categoriaExistente;
        }

        categoriaExistente.setNome(nomeNormalizado);
        categoriaExistente.setTipo(tipoCategoria);
        categoriaExistente.setDescricao(descricaoNormalizada);

        try {
            categoriaDAO.atualizar(categoriaExistente);
            return categoriaExistente;
        } catch (SQLException exception) {
            if (SqlExceptionUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    private void alterarStatus(Long categoriaId, Long usuarioId, boolean ativo) throws SQLException {
        Long idCategoria = ValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        Categoria categoriaExistente = buscarCategoriaExistente(idCategoria, idUsuario);
        if (categoriaExistente.isAtivo() == ativo) {
            return;
        }

        categoriaDAO.atualizarStatus(idCategoria, idUsuario, ativo);
    }

    private void excluir(Long categoriaId, Long usuarioId) throws SQLException {
        Long idCategoria = ValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCategoriaExistente(idCategoria, idUsuario);

        try {
            categoriaDAO.excluir(idCategoria, idUsuario);
        } catch (SQLException exception) {
            if (SqlExceptionUtils.isForeignKeyRestriction(exception)) {
                throw new RegraNegocioException(
                        "A categoria nao pode ser excluida porque possui transacoes vinculadas.",
                        exception
                );
            }
            throw exception;
        }
    }

    private Categoria buscarCategoriaExistente(Long categoriaId, Long usuarioId) throws SQLException {
        return categoriaDAO.buscarPorId(categoriaId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Categoria nao encontrada."));
    }

    private record CategoriaResultado(List<Categoria> categorias, String mensagemSucesso) {
    }
}
