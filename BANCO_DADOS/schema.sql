-- Cria o banco principal do sistema com suporte completo a UTF-8.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS controle_despesas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE controle_despesas;

CREATE USER IF NOT EXISTS 'controle_app'@'localhost'
    IDENTIFIED BY 'senha123';

ALTER USER 'controle_app'@'localhost'
    IDENTIFIED BY 'senha123';

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

-- =========================================================
-- DADOS DE TESTE PARA DEMONSTRACAO
-- =========================================================
-- Usuario do sistema:
-- E-mail: teste@controle.com
-- Senha: Teste@123
--
-- A senha abaixo esta armazenada como hash BCrypt.
-- =========================================================

START TRANSACTION;

SET @email_usuario_teste = CONVERT('teste@controle.com' USING utf8mb4) COLLATE utf8mb4_unicode_ci;

SET @usuario_teste_anterior = (
    SELECT id
    FROM usuarios
    WHERE email = @email_usuario_teste
    LIMIT 1
);

DELETE FROM movimentacoes_cofrinho
WHERE usuario_id = @usuario_teste_anterior;

DELETE FROM transacoes
WHERE usuario_id = @usuario_teste_anterior;

DELETE FROM cofrinhos
WHERE usuario_id = @usuario_teste_anterior;

DELETE FROM categorias
WHERE usuario_id = @usuario_teste_anterior;

DELETE FROM contas
WHERE usuario_id = @usuario_teste_anterior;

DELETE FROM usuarios
WHERE id = @usuario_teste_anterior;

-- ---------------------------------------------------------
-- USUARIO DE TESTE
-- ---------------------------------------------------------
-- Senha em texto para login: Teste@123

INSERT INTO usuarios (
    nome,
    email,
    senha,
    ativo
) VALUES (
    'Usuário de Teste',
    'teste@controle.com',
    '$2a$10$Q8JxkhGj/fTRz6AYYUkl2ud0nd.qfHtdt.vbzSovoopFdNwaOX2Cy',
    TRUE
);

SET @usuario_id = LAST_INSERT_ID();

-- ---------------------------------------------------------
-- CATEGORIAS DE RECEITA
-- ---------------------------------------------------------

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Salário',
    'receita',
    'Recebimento mensal do trabalho',
    TRUE
);

SET @categoria_salario_id = LAST_INSERT_ID();

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Trabalho freelance',
    'receita',
    'Receitas provenientes de serviços extras',
    TRUE
);

SET @categoria_freelance_id = LAST_INSERT_ID();

-- ---------------------------------------------------------
-- CATEGORIAS DE DESPESA
-- ---------------------------------------------------------

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Alimentação',
    'despesa',
    'Supermercado, restaurantes e lanches',
    TRUE
);

SET @categoria_alimentacao_id = LAST_INSERT_ID();

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Transporte',
    'despesa',
    'Combustível, ônibus e transporte por aplicativo',
    TRUE
);

SET @categoria_transporte_id = LAST_INSERT_ID();

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Moradia',
    'despesa',
    'Aluguel, energia, água e despesas residenciais',
    TRUE
);

SET @categoria_moradia_id = LAST_INSERT_ID();

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Lazer',
    'despesa',
    'Cinema, passeios e entretenimento',
    TRUE
);

SET @categoria_lazer_id = LAST_INSERT_ID();

INSERT INTO categorias (
    usuario_id,
    nome,
    tipo,
    descricao,
    ativo
) VALUES (
    @usuario_id,
    'Saúde',
    'despesa',
    'Consultas, exames e medicamentos',
    TRUE
);

SET @categoria_saude_id = LAST_INSERT_ID();

-- ---------------------------------------------------------
-- CONTAS
-- ---------------------------------------------------------

INSERT INTO contas (
    usuario_id,
    nome,
    tipo,
    instituicao,
    saldo_inicial,
    ativo
) VALUES (
    @usuario_id,
    'Conta-corrente principal',
    'conta_corrente',
    'Banco Demonstração',
    1500.00,
    TRUE
);

SET @conta_corrente_id = LAST_INSERT_ID();

INSERT INTO contas (
    usuario_id,
    nome,
    tipo,
    instituicao,
    saldo_inicial,
    ativo
) VALUES (
    @usuario_id,
    'Carteira',
    'carteira',
    NULL,
    200.00,
    TRUE
);

SET @carteira_id = LAST_INSERT_ID();

INSERT INTO contas (
    usuario_id,
    nome,
    tipo,
    instituicao,
    saldo_inicial,
    ativo
) VALUES (
    @usuario_id,
    'Poupança',
    'poupanca',
    'Banco Demonstração',
    3000.00,
    TRUE
);

SET @poupanca_id = LAST_INSERT_ID();

-- ---------------------------------------------------------
-- RECEITAS
-- ---------------------------------------------------------

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_salario_id,
    @conta_corrente_id,
    'receita',
    'Salário mensal',
    4500.00,
    CURRENT_DATE,
    'recebido',
    'Salário recebido no mês atual'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_freelance_id,
    @conta_corrente_id,
    'receita',
    'Desenvolvimento de sistema',
    800.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY),
    'pendente',
    'Pagamento aguardando recebimento'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_freelance_id,
    @conta_corrente_id,
    'receita',
    'Manutenção de site',
    600.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 35 DAY),
    'recebido',
    'Serviço realizado no mês anterior'
);

-- ---------------------------------------------------------
-- DESPESAS
-- ---------------------------------------------------------

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_moradia_id,
    @conta_corrente_id,
    'despesa',
    'Aluguel',
    1200.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY),
    'pago',
    'Aluguel da residência'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_alimentacao_id,
    @conta_corrente_id,
    'despesa',
    'Compras do supermercado',
    350.75,
    DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY),
    'pago',
    'Compras de alimentos e produtos domésticos'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_transporte_id,
    @carteira_id,
    'despesa',
    'Transporte por aplicativo',
    120.40,
    DATE_SUB(CURRENT_DATE, INTERVAL 3 DAY),
    'pago',
    'Deslocamentos realizados durante a semana'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_lazer_id,
    @carteira_id,
    'despesa',
    'Cinema',
    80.00,
    CURRENT_DATE,
    'pendente',
    'Compra programada para o fim de semana'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_saude_id,
    @conta_corrente_id,
    'despesa',
    'Consulta médica',
    250.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY),
    'cancelado',
    'Consulta cancelada e não cobrada'
);

INSERT INTO transacoes (
    usuario_id,
    categoria_id,
    conta_id,
    tipo,
    descricao,
    valor,
    data_transacao,
    status,
    observacoes
) VALUES (
    @usuario_id,
    @categoria_moradia_id,
    @conta_corrente_id,
    'despesa',
    'Conta de energia',
    180.50,
    DATE_SUB(CURRENT_DATE, INTERVAL 32 DAY),
    'pago',
    'Conta de energia do mês anterior'
);

-- ---------------------------------------------------------
-- COFRINHO: NOTEBOOK
-- Valor atual esperado:
-- 1000 + 750 - 100 = 1650
-- ---------------------------------------------------------

INSERT INTO cofrinhos (
    usuario_id,
    nome,
    descricao,
    valor_meta,
    data_limite,
    status
) VALUES (
    @usuario_id,
    'Comprar um notebook',
    'Meta para comprar um notebook novo',
    5000.00,
    DATE_ADD(CURRENT_DATE, INTERVAL 180 DAY),
    'em_andamento'
);

SET @cofrinho_notebook_id = LAST_INSERT_ID();

INSERT INTO movimentacoes_cofrinho (
    cofrinho_id,
    usuario_id,
    tipo,
    valor,
    data_movimentacao,
    observacao
) VALUES
(
    @cofrinho_notebook_id,
    @usuario_id,
    'deposito',
    1000.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY),
    'Primeiro depósito'
),
(
    @cofrinho_notebook_id,
    @usuario_id,
    'deposito',
    750.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY),
    'Depósito mensal'
),
(
    @cofrinho_notebook_id,
    @usuario_id,
    'retirada',
    100.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY),
    'Retirada emergencial'
);

-- ---------------------------------------------------------
-- COFRINHO: RESERVA DE EMERGENCIA
-- Valor atual esperado:
-- 2000 + 1500 = 3500
-- Meta: 3000
-- Status: concluido
-- ---------------------------------------------------------

INSERT INTO cofrinhos (
    usuario_id,
    nome,
    descricao,
    valor_meta,
    data_limite,
    status
) VALUES (
    @usuario_id,
    'Reserva de emergência',
    'Valor reservado para despesas inesperadas',
    3000.00,
    DATE_ADD(CURRENT_DATE, INTERVAL 365 DAY),
    'concluido'
);

SET @cofrinho_reserva_id = LAST_INSERT_ID();

INSERT INTO movimentacoes_cofrinho (
    cofrinho_id,
    usuario_id,
    tipo,
    valor,
    data_movimentacao,
    observacao
) VALUES
(
    @cofrinho_reserva_id,
    @usuario_id,
    'deposito',
    2000.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY),
    'Valor inicial da reserva'
),
(
    @cofrinho_reserva_id,
    @usuario_id,
    'deposito',
    1500.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY),
    'Complemento da reserva'
);

-- ---------------------------------------------------------
-- COFRINHO: VIAGEM
-- Mantido cancelado para demonstracao do filtro de status.
-- ---------------------------------------------------------

INSERT INTO cofrinhos (
    usuario_id,
    nome,
    descricao,
    valor_meta,
    data_limite,
    status
) VALUES (
    @usuario_id,
    'Viagem de férias',
    'Meta cancelada para demonstrar os diferentes status',
    4000.00,
    DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY),
    'cancelado'
);

SET @cofrinho_viagem_id = LAST_INSERT_ID();

INSERT INTO movimentacoes_cofrinho (
    cofrinho_id,
    usuario_id,
    tipo,
    valor,
    data_movimentacao,
    observacao
) VALUES (
    @cofrinho_viagem_id,
    @usuario_id,
    'deposito',
    500.00,
    DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY),
    'Depósito realizado antes do cancelamento'
);

COMMIT;

