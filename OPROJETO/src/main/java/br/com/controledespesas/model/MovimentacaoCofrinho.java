package br.com.controledespesas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MovimentacaoCofrinho {

    private Long id;
    private Long cofrinhoId;
    private Long usuarioId;
    private TipoMovimentacaoCofrinho tipo;
    private BigDecimal valor;
    private LocalDate dataMovimentacao;
    private String observacao;
    private LocalDateTime criadoEm;

    public MovimentacaoCofrinho() {
    }

    public MovimentacaoCofrinho(Long id, Long cofrinhoId, Long usuarioId,
                                TipoMovimentacaoCofrinho tipo, BigDecimal valor,
                                LocalDate dataMovimentacao, String observacao,
                                LocalDateTime criadoEm) {
        this.id = id;
        this.cofrinhoId = cofrinhoId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.valor = valor;
        this.dataMovimentacao = dataMovimentacao;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCofrinhoId() {
        return cofrinhoId;
    }

    public void setCofrinhoId(Long cofrinhoId) {
        this.cofrinhoId = cofrinhoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public TipoMovimentacaoCofrinho getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacaoCofrinho tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(LocalDate dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public String toString() {
        return "MovimentacaoCofrinho{"
                + "id=" + id
                + ", cofrinhoId=" + cofrinhoId
                + ", usuarioId=" + usuarioId
                + ", tipo=" + tipo
                + ", valor=" + valor
                + ", dataMovimentacao=" + dataMovimentacao
                + ", observacao='" + observacao + '\''
                + ", criadoEm=" + criadoEm
                + '}';
    }
}
