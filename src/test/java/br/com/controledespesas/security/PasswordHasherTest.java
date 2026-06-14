package br.com.controledespesas.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void shouldGenerateHashDifferentFromPlainPassword() {
        String hash = passwordHasher.gerarHash("Senha123");

        assertNotEquals("Senha123", hash);
    }

    @Test
    void shouldAcceptCorrectPasswordAndRejectIncorrectPassword() {
        String hash = passwordHasher.gerarHash("Senha123");

        assertTrue(passwordHasher.verificar("Senha123", hash));
        assertFalse(passwordHasher.verificar("SenhaErrada", hash));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String primeiroHash = passwordHasher.gerarHash("Senha123");
        String segundoHash = passwordHasher.gerarHash("Senha123");

        assertNotEquals(primeiroHash, segundoHash);
    }

    @Test
    void shouldReturnFalseForInvalidHashWithoutThrowingSensitiveErrors() {
        assertFalse(passwordHasher.verificar("Senha123", "hash-invalido"));
    }
}
