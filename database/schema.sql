-- Cria o banco principal do sistema com suporte completo a UTF-8.
CREATE DATABASE IF NOT EXISTS controle_despesas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE controle_despesas;

-- Altere a senha abaixo antes de usar em producao.
-- Ela deve ser mantida igual ao valor configurado em DB_PASSWORD no arquivo .env.
CREATE USER IF NOT EXISTS 'controle_app'@'localhost'
    IDENTIFIED BY 'troque_esta_senha';

-- Concede acesso apenas ao banco da aplicacao.
GRANT ALL PRIVILEGES ON controle_despesas.* TO 'controle_app'@'localhost';

FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT chk_usuarios_ativo CHECK (ativo IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo ENUM('receita', 'despesa') NOT NULL,
    descricao VARCHAR(255) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_categorias_usuario_nome_tipo UNIQUE (usuario_id, nome, tipo),
    CONSTRAINT chk_categorias_ativo CHECK (ativo IN (0, 1)),
    CONSTRAINT fk_categorias_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contas (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo ENUM('carteira', 'conta_corrente', 'poupanca', 'conta_digital', 'outro') NOT NULL,
    instituicao VARCHAR(150) NULL,
    saldo_inicial DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_contas_usuario_nome UNIQUE (usuario_id, nome),
    CONSTRAINT chk_contas_saldo_inicial CHECK (saldo_inicial >= 0.00),
    CONSTRAINT chk_contas_ativo CHECK (ativo IN (0, 1)),
    CONSTRAINT fk_contas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- A camada Java devera validar que:
-- 1. categoria_id pertence ao mesmo usuario_id da transacao;
-- 2. conta_id pertence ao mesmo usuario_id da transacao;
-- 3. o tipo da categoria corresponde ao tipo da transacao.
CREATE TABLE IF NOT EXISTS transacoes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    categoria_id BIGINT UNSIGNED NOT NULL,
    conta_id BIGINT UNSIGNED NOT NULL,
    tipo ENUM('receita', 'despesa') NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_transacao DATE NOT NULL,
    status ENUM('pendente', 'pago', 'recebido', 'cancelado') NOT NULL DEFAULT 'pendente',
    observacoes TEXT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_transacoes_valor CHECK (valor > 0.00),
    CONSTRAINT chk_transacoes_status_tipo CHECK (
        (tipo = 'despesa' AND status IN ('pendente', 'pago', 'cancelado'))
        OR (tipo = 'receita' AND status IN ('pendente', 'recebido', 'cancelado'))
    ),
    CONSTRAINT fk_transacoes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_transacoes_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_transacoes_conta
        FOREIGN KEY (conta_id) REFERENCES contas (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_transacoes_usuario (usuario_id),
    INDEX idx_transacoes_usuario_data (usuario_id, data_transacao),
    INDEX idx_transacoes_usuario_tipo (usuario_id, tipo),
    INDEX idx_transacoes_usuario_status (usuario_id, status),
    INDEX idx_transacoes_categoria (categoria_id),
    INDEX idx_transacoes_conta (conta_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cofrinhos (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT NULL,
    valor_meta DECIMAL(12,2) NOT NULL,
    data_limite DATE NULL,
    status ENUM('em_andamento', 'concluido', 'cancelado') NOT NULL DEFAULT 'em_andamento',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_cofrinhos_valor_meta CHECK (valor_meta > 0.00),
    CONSTRAINT fk_cofrinhos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_cofrinhos_usuario (usuario_id),
    INDEX idx_cofrinhos_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- A camada Java devera validar que:
-- 1. usuario_id e o proprietario do cofrinho;
-- 2. retiradas nao ultrapassam o saldo calculado do cofrinho.
CREATE TABLE IF NOT EXISTS movimentacoes_cofrinho (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cofrinho_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    tipo ENUM('deposito', 'retirada') NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    data_movimentacao DATE NOT NULL,
    observacao TEXT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimentacoes_cofrinho_valor CHECK (valor > 0.00),
    CONSTRAINT fk_movimentacoes_cofrinho
        FOREIGN KEY (cofrinho_id) REFERENCES cofrinhos (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_movimentacoes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_movimentacoes_cofrinho (cofrinho_id),
    INDEX idx_movimentacoes_usuario (usuario_id),
    INDEX idx_movimentacoes_data (data_movimentacao),
    INDEX idx_movimentacoes_cofrinho_data (cofrinho_id, data_movimentacao)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Consultas de verificacao uteis durante o desenvolvimento:
-- SHOW TABLES;
-- DESCRIBE usuarios;
-- DESCRIBE categorias;
-- DESCRIBE contas;
-- DESCRIBE transacoes;
-- DESCRIBE cofrinhos;
-- DESCRIBE movimentacoes_cofrinho;
-- SELECT
--     TABLE_NAME,
--     CONSTRAINT_NAME,
--     COLUMN_NAME,
--     REFERENCED_TABLE_NAME,
--     REFERENCED_COLUMN_NAME
-- FROM information_schema.KEY_COLUMN_USAGE
-- WHERE TABLE_SCHEMA = 'controle_despesas'
--   AND REFERENCED_TABLE_NAME IS NOT NULL
-- ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;
