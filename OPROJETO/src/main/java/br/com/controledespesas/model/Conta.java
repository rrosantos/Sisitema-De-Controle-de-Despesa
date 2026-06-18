package br.com.controledespesas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa a entidade Conta manipulada pelo sistema.
 */
public class Conta {

    private Long id;
    private Long usuarioId;
    private String nome;
    private TipoConta tipo;
    private String instituicao;
    private BigDecimal saldoInicial;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Conta() {
    }

    public Conta(Long id, Long usuarioId, String nome, TipoConta tipo, String instituicao,
                 BigDecimal saldoInicial, boolean ativo, LocalDateTime criadoEm,
                 LocalDateTime atualizadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.tipo = tipo;
        this.instituicao = instituicao;
        this.saldoInicial = saldoInicial;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(String instituicao) {
        this.instituicao = instituicao;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Conta{"
                + "id=" + id
                + ", usuarioId=" + usuarioId
                + ", nome='" + nome + '\''
                + ", tipo=" + tipo
                + ", instituicao='" + instituicao + '\''
                + ", saldoInicial=" + saldoInicial
                + ", ativo=" + ativo
                + ", criadoEm=" + criadoEm
                + ", atualizadoEm=" + atualizadoEm
                + '}';
    }
}
