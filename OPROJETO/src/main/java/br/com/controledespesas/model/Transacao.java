package br.com.controledespesas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa a entidade Transacao manipulada pelo sistema.
 */
public class Transacao {

    private Long id;
    private Long usuarioId;
    private Long categoriaId;
    private Long contaId;
    private TipoTransacao tipo;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataTransacao;
    private StatusTransacao status;
    private String observacoes;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Transacao() {
    }

    public Transacao(Long id, Long usuarioId, Long categoriaId, Long contaId, TipoTransacao tipo,
                     String descricao, BigDecimal valor, LocalDate dataTransacao,
                     StatusTransacao status, String observacoes,
                     LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
        this.contaId = contaId;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.dataTransacao = dataTransacao;
        this.status = status;
        this.observacoes = observacoes;
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

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getContaId() {
        return contaId;
    }

    public void setContaId(Long contaId) {
        this.contaId = contaId;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataTransacao() {
        return dataTransacao;
    }

    public void setDataTransacao(LocalDate dataTransacao) {
        this.dataTransacao = dataTransacao;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public void setStatus(StatusTransacao status) {
        this.status = status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
        return "Transacao{"
                + "id=" + id
                + ", usuarioId=" + usuarioId
                + ", categoriaId=" + categoriaId
                + ", contaId=" + contaId
                + ", tipo=" + tipo
                + ", descricao='" + descricao + '\''
                + ", valor=" + valor
                + ", dataTransacao=" + dataTransacao
                + ", status=" + status
                + ", observacoes='" + observacoes + '\''
                + ", criadoEm=" + criadoEm
                + ", atualizadoEm=" + atualizadoEm
                + '}';
    }
}
