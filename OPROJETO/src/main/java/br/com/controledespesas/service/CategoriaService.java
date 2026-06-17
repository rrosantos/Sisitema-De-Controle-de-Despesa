package br.com.controledespesas.service;

import br.com.controledespesas.dao.CategoriaDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Categoria;
import br.com.controledespesas.model.TipoCategoria;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class CategoriaService {

    private static final int MAX_NOME = 100;
    private static final int MAX_DESCRICAO = 255;
    private static final String MENSAGEM_DUPLICIDADE = "Ja existe uma categoria com este nome e tipo.";

    private final CategoriaDAO categoriaDAO;

    public CategoriaService() {
        this(new CategoriaDAO());
    }

    public CategoriaService(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO nao pode ser nulo.");
    }

    public Categoria cadastrar(Long usuarioId, String nome, TipoCategoria tipo, String descricao) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome da categoria", MAX_NOME);
        TipoCategoria tipoCategoria = ServiceValidationUtils.requireValue(tipo, "Tipo da categoria");
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(descricao, "Descricao da categoria", MAX_DESCRICAO);

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
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    public Categoria buscarPorId(Long categoriaId, Long usuarioId) throws SQLException {
        return buscarCategoriaExistente(categoriaId, usuarioId);
    }

    public List<Categoria> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return categoriaDAO.listarPorUsuario(idUsuario);
    }

    public List<Categoria> listarAtivas(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return categoriaDAO.listarAtivasPorUsuario(idUsuario);
    }

    public List<Categoria> listarPorTipo(Long usuarioId, TipoCategoria tipo) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        TipoCategoria tipoCategoria = ServiceValidationUtils.requireValue(tipo, "Tipo da categoria");
        return categoriaDAO.listarPorUsuarioETipo(idUsuario, tipoCategoria);
    }

    public Categoria atualizar(Long categoriaId, Long usuarioId, String nome, TipoCategoria tipo, String descricao)
            throws SQLException {
        Long idCategoria = ServiceValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome da categoria", MAX_NOME);
        TipoCategoria tipoCategoria = ServiceValidationUtils.requireValue(tipo, "Tipo da categoria");
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(descricao, "Descricao da categoria", MAX_DESCRICAO);

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
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    public void alterarStatus(Long categoriaId, Long usuarioId, boolean ativo) throws SQLException {
        Long idCategoria = ServiceValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        Categoria categoriaExistente = buscarCategoriaExistente(idCategoria, idUsuario);
        if (categoriaExistente.isAtivo() == ativo) {
            return;
        }

        categoriaDAO.atualizarStatus(idCategoria, idUsuario, ativo);
    }

    public void excluir(Long categoriaId, Long usuarioId) throws SQLException {
        Long idCategoria = ServiceValidationUtils.requireId(categoriaId, "ID da categoria");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCategoriaExistente(idCategoria, idUsuario);

        try {
            categoriaDAO.excluir(idCategoria, idUsuario);
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isForeignKeyRestriction(exception)) {
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
}
