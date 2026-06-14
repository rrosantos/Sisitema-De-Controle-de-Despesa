package br.com.controledespesas.service;

import br.com.controledespesas.dao.CofrinhoDAO;
import br.com.controledespesas.dao.MovimentacaoCofrinhoDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Cofrinho;
import br.com.controledespesas.model.StatusCofrinho;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class CofrinhoService {

    private static final int MAX_NOME = 150;
    private static final int MAX_DESCRICAO = 65535;

    private final CofrinhoDAO cofrinhoDAO;
    private final MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO;

    public CofrinhoService() {
        this(new CofrinhoDAO(), new MovimentacaoCofrinhoDAO());
    }

    public CofrinhoService(CofrinhoDAO cofrinhoDAO, MovimentacaoCofrinhoDAO movimentacaoCofrinhoDAO) {
        this.cofrinhoDAO = Objects.requireNonNull(cofrinhoDAO, "cofrinhoDAO nao pode ser nulo.");
        this.movimentacaoCofrinhoDAO =
                Objects.requireNonNull(movimentacaoCofrinhoDAO, "movimentacaoCofrinhoDAO nao pode ser nulo.");
    }

    public Cofrinho cadastrar(Long usuarioId, String nome, String descricao, BigDecimal valorMeta, LocalDate dataLimite)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome do cofrinho", MAX_NOME);
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(descricao, "Descricao do cofrinho", MAX_DESCRICAO);
        BigDecimal valorMetaNormalizado =
                ServiceValidationUtils.normalizeMonetaryValue(valorMeta, "Valor da meta", false);

        Cofrinho cofrinho = new Cofrinho();
        cofrinho.setUsuarioId(idUsuario);
        cofrinho.setNome(nomeNormalizado);
        cofrinho.setDescricao(descricaoNormalizada);
        cofrinho.setValorMeta(valorMetaNormalizado);
        cofrinho.setDataLimite(dataLimite);
        cofrinho.setStatus(StatusCofrinho.EM_ANDAMENTO);

        cofrinhoDAO.inserir(cofrinho);
        return cofrinho;
    }

    public Cofrinho buscarPorId(Long cofrinhoId, Long usuarioId) throws SQLException {
        return buscarCofrinhoExistente(cofrinhoId, usuarioId);
    }

    public List<Cofrinho> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return cofrinhoDAO.listarPorUsuario(idUsuario);
    }

    public List<Cofrinho> listarPorStatus(Long usuarioId, StatusCofrinho status) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        StatusCofrinho statusCofrinho = ServiceValidationUtils.requireValue(status, "Status do cofrinho");
        return cofrinhoDAO.listarPorUsuarioEStatus(idUsuario, statusCofrinho);
    }

    public Cofrinho atualizar(Long cofrinhoId, Long usuarioId, String nome, String descricao, BigDecimal valorMeta,
                              LocalDate dataLimite) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome do cofrinho", MAX_NOME);
        String descricaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(descricao, "Descricao do cofrinho", MAX_DESCRICAO);
        BigDecimal valorMetaNormalizado =
                ServiceValidationUtils.normalizeMonetaryValue(valorMeta, "Valor da meta", false);

        Cofrinho cofrinhoExistente = buscarCofrinhoExistente(idCofrinho, idUsuario);
        if (Objects.equals(cofrinhoExistente.getNome(), nomeNormalizado)
                && Objects.equals(cofrinhoExistente.getDescricao(), descricaoNormalizada)
                && Objects.equals(cofrinhoExistente.getValorMeta(), valorMetaNormalizado)
                && Objects.equals(cofrinhoExistente.getDataLimite(), dataLimite)) {
            return cofrinhoExistente;
        }

        cofrinhoExistente.setNome(nomeNormalizado);
        cofrinhoExistente.setDescricao(descricaoNormalizada);
        cofrinhoExistente.setValorMeta(valorMetaNormalizado);
        cofrinhoExistente.setDataLimite(dataLimite);

        cofrinhoDAO.atualizar(cofrinhoExistente);
        return cofrinhoExistente;
    }

    public void alterarStatus(Long cofrinhoId, Long usuarioId, StatusCofrinho status) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        StatusCofrinho statusCofrinho = ServiceValidationUtils.requireValue(status, "Status do cofrinho");
        Cofrinho cofrinhoExistente = buscarCofrinhoExistente(idCofrinho, idUsuario);
        if (cofrinhoExistente.getStatus() == statusCofrinho) {
            return;
        }

        cofrinhoDAO.atualizarStatus(idCofrinho, idUsuario, statusCofrinho);
    }

    public void excluir(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarCofrinhoExistente(idCofrinho, idUsuario);
        cofrinhoDAO.excluir(idCofrinho, idUsuario);
    }

    public BigDecimal consultarValorAtual(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return movimentacaoCofrinhoDAO.calcularValorAtual(idCofrinho, idUsuario)
                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));
    }

    public BigDecimal consultarPercentualProgresso(Long cofrinhoId, Long usuarioId) throws SQLException {
        Long idCofrinho = ServiceValidationUtils.requireId(cofrinhoId, "ID do cofrinho");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return movimentacaoCofrinhoDAO.calcularPercentualProgresso(idCofrinho, idUsuario)
                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));
    }

    private Cofrinho buscarCofrinhoExistente(Long cofrinhoId, Long usuarioId) throws SQLException {
        return cofrinhoDAO.buscarPorId(cofrinhoId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Cofrinho nao encontrado."));
    }
}
