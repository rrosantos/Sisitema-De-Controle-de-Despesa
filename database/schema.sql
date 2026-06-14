-- Cria o banco principal do sistema com suporte completo a UTF-8.
CREATE DATABASE IF NOT EXISTS controle_despesas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Altere a senha abaixo antes de usar em producao.
-- Ela deve ser mantida igual ao valor configurado em DB_PASSWORD no arquivo .env.
CREATE USER IF NOT EXISTS 'controle_app'@'localhost'
    IDENTIFIED BY 'password';

-- Concede acesso apenas ao banco da aplicacao.
GRANT ALL PRIVILEGES ON controle_despesas.* TO 'controle_app'@'localhost';

FLUSH PRIVILEGES;
