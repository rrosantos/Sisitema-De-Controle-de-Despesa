package br.com.controledespesas.service;

import br.com.controledespesas.dao.ContaDAO;
import br.com.controledespesas.exception.RegraNegocioException;
import br.com.controledespesas.model.Conta;
import br.com.controledespesas.model.TipoConta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class ContaService {

    private static final int MAX_NOME = 100;
    private static final int MAX_INSTITUICAO = 150;
    private static final String MENSAGEM_DUPLICIDADE = "Ja existe uma conta com este nome.";

    private final ContaDAO contaDAO;

    public ContaService() {
        this(new ContaDAO());
    }

    public ContaService(ContaDAO contaDAO) {
        this.contaDAO = Objects.requireNonNull(contaDAO, "contaDAO nao pode ser nulo.");
    }

    public Conta cadastrar(Long usuarioId, String nome, TipoConta tipo, String instituicao, BigDecimal saldoInicial)
            throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome da conta", MAX_NOME);
        TipoConta tipoConta = ServiceValidationUtils.requireValue(tipo, "Tipo da conta");
        String instituicaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(instituicao, "Instituicao", MAX_INSTITUICAO);
        BigDecimal saldoNormalizado =
                ServiceValidationUtils.normalizeMonetaryValue(saldoInicial, "Saldo inicial", true);

        if (contaDAO.nomeExiste(idUsuario, nomeNormalizado)) {
            throw new RegraNegocioException(MENSAGEM_DUPLICIDADE);
        }

        Conta conta = new Conta();
        conta.setUsuarioId(idUsuario);
        conta.setNome(nomeNormalizado);
        conta.setTipo(tipoConta);
        conta.setInstituicao(instituicaoNormalizada);
        conta.setSaldoInicial(saldoNormalizado);
        conta.setAtivo(true);

        try {
            contaDAO.inserir(conta);
            return conta;
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    public Conta buscarPorId(Long contaId, Long usuarioId) throws SQLException {
        return buscarContaExistente(contaId, usuarioId);
    }

    public List<Conta> listarPorUsuario(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return contaDAO.listarPorUsuario(idUsuario);
    }

    public List<Conta> listarAtivas(Long usuarioId) throws SQLException {
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return contaDAO.listarAtivasPorUsuario(idUsuario);
    }

    public Conta atualizar(Long contaId, Long usuarioId, String nome, TipoConta tipo, String instituicao,
                           BigDecimal saldoInicial) throws SQLException {
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        String nomeNormalizado = ServiceValidationUtils.normalizeRequiredText(nome, "Nome da conta", MAX_NOME);
        TipoConta tipoConta = ServiceValidationUtils.requireValue(tipo, "Tipo da conta");
        String instituicaoNormalizada =
                ServiceValidationUtils.normalizeOptionalText(instituicao, "Instituicao", MAX_INSTITUICAO);
        BigDecimal saldoNormalizado =
                ServiceValidationUtils.normalizeMonetaryValue(saldoInicial, "Saldo inicial", true);

        Conta contaExistente = buscarContaExistente(idConta, idUsuario);
        if (contaDAO.nomeExisteParaOutraConta(idUsuario, nomeNormalizado, idConta)) {
            throw new RegraNegocioException(MENSAGEM_DUPLICIDADE);
        }

        if (Objects.equals(contaExistente.getNome(), nomeNormalizado)
                && contaExistente.getTipo() == tipoConta
                && Objects.equals(contaExistente.getInstituicao(), instituicaoNormalizada)
                && Objects.equals(contaExistente.getSaldoInicial(), saldoNormalizado)) {
            return contaExistente;
        }

        contaExistente.setNome(nomeNormalizado);
        contaExistente.setTipo(tipoConta);
        contaExistente.setInstituicao(instituicaoNormalizada);
        contaExistente.setSaldoInicial(saldoNormalizado);

        try {
            contaDAO.atualizar(contaExistente);
            return contaExistente;
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isDuplicateKey(exception)) {
                throw new RegraNegocioException(MENSAGEM_DUPLICIDADE, exception);
            }
            throw exception;
        }
    }

    public void alterarStatus(Long contaId, Long usuarioId, boolean ativo) throws SQLException {
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        Conta contaExistente = buscarContaExistente(idConta, idUsuario);
        if (contaExistente.isAtivo() == ativo) {
            return;
        }

        contaDAO.atualizarStatus(idConta, idUsuario, ativo);
    }

    public void excluir(Long contaId, Long usuarioId) throws SQLException {
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        buscarContaExistente(idConta, idUsuario);

        try {
            contaDAO.excluir(idConta, idUsuario);
        } catch (SQLException exception) {
            if (ServiceSqlUtils.isForeignKeyRestriction(exception)) {
                throw new RegraNegocioException(
                        "A conta nao pode ser excluida porque possui transacoes vinculadas.",
                        exception
                );
            }
            throw exception;
        }
    }

    public BigDecimal consultarSaldoAtual(Long contaId, Long usuarioId) throws SQLException {
        Long idConta = ServiceValidationUtils.requireId(contaId, "ID da conta");
        Long idUsuario = ServiceValidationUtils.requireId(usuarioId, "ID do usuario");
        return contaDAO.calcularSaldoAtual(idConta, idUsuario)
                .orElseThrow(() -> new RegraNegocioException("Conta nao encontrada."));
    }

    private Conta buscarContaExistente(Long contaId, Long usuarioId) throws SQLException {
        return contaDAO.buscarPorId(contaId, usuarioId)
                .orElseThrow(() -> new RegraNegocioException("Conta nao encontrada."));
    }
}
