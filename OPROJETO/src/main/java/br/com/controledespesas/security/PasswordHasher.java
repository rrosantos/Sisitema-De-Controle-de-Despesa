package br.com.controledespesas.security;

import br.com.controledespesas.exception.ValidacaoException;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Encapsula geracao e verificacao de hashes de senha.
 */
public class PasswordHasher {

    private static final int BCRYPT_COST = 10;

    public String gerarHash(String senha) {
        String senhaNormalizada = validarSenhaObrigatoria(senha);
        return BCrypt.hashpw(senhaNormalizada, BCrypt.gensalt(BCRYPT_COST));
    }

    public boolean verificar(String senha, String hash) {
        String senhaNormalizada = validarSenhaObrigatoria(senha);
        if (hash == null || hash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(senhaNormalizada, hash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String validarSenhaObrigatoria(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new ValidacaoException("A senha e obrigatoria.");
        }
        return senha;
    }
}
