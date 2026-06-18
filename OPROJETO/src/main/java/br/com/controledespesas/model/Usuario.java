package br.com.controledespesas.model;

import java.time.LocalDateTime;

/**
 * Representa a entidade Usuario manipulada pelo sistema.
 */
public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Usuario() {
    }

    public Usuario(Long id, String nome, String email, String senhaHash, boolean ativo,
                   LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
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
        return "Usuario{"
                + "id=" + id
                + ", nome='" + nome + '\''
                + ", email='" + email + '\''
                + ", ativo=" + ativo
                + ", criadoEm=" + criadoEm
                + ", atualizadoEm=" + atualizadoEm
                + '}';
    }
}
