package br.com.controledespesas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Cofrinho {

    private Long id;
    private Long usuarioId;
    private String nome;
    private String descricao;
    private BigDecimal valorMeta;
    private LocalDate dataLimite;
    private StatusCofrinho status;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Cofrinho() {
    }

    public Cofrinho(Long id, Long usuarioId, String nome, String descricao, BigDecimal valorMeta,
                    LocalDate dataLimite, StatusCofrinho status,
                    LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.descricao = descricao;
        this.valorMeta = valorMeta;
        this.dataLimite = dataLimite;
        this.status = status;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorMeta() {
        return valorMeta;
    }

    public void setValorMeta(BigDecimal valorMeta) {
        this.valorMeta = valorMeta;
    }

    public LocalDate getDataLimite() {
        return dataLimite;
    }

    public void setDataLimite(LocalDate dataLimite) {
        this.dataLimite = dataLimite;
    }

    public StatusCofrinho getStatus() {
        return status;
    }

    public void setStatus(StatusCofrinho status) {
        this.status = status;
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
        return "Cofrinho{"
                + "id=" + id
                + ", usuarioId=" + usuarioId
                + ", nome='" + nome + '\''
                + ", descricao='" + descricao + '\''
                + ", valorMeta=" + valorMeta
                + ", dataLimite=" + dataLimite
                + ", status=" + status
                + ", criadoEm=" + criadoEm
                + ", atualizadoEm=" + atualizadoEm
                + '}';
    }
}
