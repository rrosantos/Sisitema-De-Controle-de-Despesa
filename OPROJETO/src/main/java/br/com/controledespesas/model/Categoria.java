package br.com.controledespesas.model;

import java.time.LocalDateTime;

/**
 * Representa a entidade Categoria manipulada pelo sistema.
 */
public class Categoria {

    private Long id;
    private Long usuarioId;
    private String nome;
    private TipoCategoria tipo;
    private String descricao;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Categoria() {
    }

    public Categoria(Long id, Long usuarioId, String nome, TipoCategoria tipo, String descricao,
                     boolean ativo, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.tipo = tipo;
        this.descricao = descricao;
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

    public TipoCategoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoCategoria tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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
        return "Categoria{"
                + "id=" + id
                + ", usuarioId=" + usuarioId
                + ", nome='" + nome + '\''
                + ", tipo=" + tipo
                + ", descricao='" + descricao + '\''
                + ", ativo=" + ativo
                + ", criadoEm=" + criadoEm
                + ", atualizadoEm=" + atualizadoEm
                + '}';
    }
}
