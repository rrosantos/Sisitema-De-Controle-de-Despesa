# Sistema de Controle de Despesas Pessoais

Aplicacao desktop em Java 21 com Swing, JDBC e MySQL para controle de receitas, despesas, contas, categorias, cofrinhos e dashboard financeiro por usuario autenticado.

## Funcionalidades

- Cadastro e login com senha protegida por BCrypt.
- Isolamento completo dos dados por `usuario_id`.
- CRUD de categorias de receita e despesa.
- CRUD de contas com saldo inicial persistido, saldo atual calculado, pesquisa por campo e listagem com ordenacao explicita.
- A listagem de contas pode ser ordenada por nome crescente, nome decrescente, maior saldo atual ou menor saldo atual.
- CRUD de transacoes com filtros, resumo do periodo e validacoes por tipo e status.
- CRUD de cofrinhos com depositos, retiradas, historico e progresso de meta.
- Dashboard financeiro com indicadores, atalhos de periodo e visoes resumidas.

## Stack e arquitetura

- Java 21
- Swing
- Maven
- MySQL 8
- JDBC
- dotenv-java
- JUnit 5 e Mockito

Camadas principais:

- `view`: renderizacao Swing, dialogs e componentes reutilizaveis.
- `controller`: fluxo da interface, navegacao e tarefas assincronas.
- `service`: regras de negocio, validacoes e coordenacao transacional.
- `dao`: persistencia JDBC com `PreparedStatement` e `try-with-resources`.
- `model` e `dto`: entidades, enums e objetos de transferencia.
- `config`, `database`, `session` e `security`: configuracao, conexao, sessao e senha.

## Requisitos

- Java 21
- Maven 3.8+
- MySQL 8

## Configuracao rapida

1. Crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

2. Ajuste as credenciais conforme o seu ambiente:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=controle_despesas
DB_USER=controle_app
DB_PASSWORD=troque_esta_senha
```

3. Crie o banco e o usuario executando o schema:

```bash
sudo mysql < ../BANCO_DADOS/schema.sql
```

Se o `root` do MySQL estiver autenticando por `sudo`, use:

```bash
sudo mysql < ../BANCO_DADOS/schema.sql
```

4. Valide o acesso do usuario da aplicacao:

```bash
mysql -u controle_app -p controle_despesas
```

## Execucao em desenvolvimento

Compilar:

```bash
mvn clean compile
```

Executar testes:

```bash
mvn clean test
```

Abrir a aplicacao:

```bash
mvn exec:java
```

## Empacotamento do jar

Gerar o jar executavel com dependencias:

```bash
mvn clean package
```

Jar gerado:

```text
target/sistema-controle-despesas-1.0.0-SNAPSHOT-app.jar
```

Executar com o script auxiliar:

```bash
./run.sh
```

Ou executar manualmente:

```bash
java -jar target/sistema-controle-despesas-1.0.0-SNAPSHOT-app.jar
```

Importante:

- O arquivo `.env` nao e empacotado no jar.
- Execute o comando a partir de uma pasta que contenha o `.env`.
- `.env` continua ignorado pelo Git; apenas `.env.example` deve ser versionado.

## Estrutura do projeto

```text
database/
src/
  main/java/br/com/controledespesas/
    app/
    config/
    controller/
    dao/
    database/
    dto/
    exception/
    model/
    security/
    service/
    session/
    util/
    view/
  test/java/br/com/controledespesas/
docs/
  entrega/
```

## Regras importantes

- Valores monetarios usam `BigDecimal`.
- Saldos atuais de contas e cofrinhos sao calculados, sem armazenamento redundante.
- Todas as consultas e alteracoes financeiras restringem acesso por `usuario_id`.
- Transacoes e movimentacoes de cofrinho usam controle transacional com `commit` e `rollback`.
- Categorias e contas com historico financeiro preservam integridade referencial.

## Documentacao de entrega

- `docs/entrega/INSTALACAO.md`
- `docs/entrega/ROTEIRO_DEMONSTRACAO.md`
- `docs/entrega/CHECKLIST_ENTREGA.md`

## Solucao de problemas

- Confirme que o MySQL esta ativo e que o `.env` aponta para as mesmas credenciais do banco.
- Se `mysql -u root -p` falhar no Ubuntu, tente `sudo mysql < ../BANCO_DADOS/schema.sql`.
- Se aparecer erro com `controle_app`, redefina a senha no MySQL e atualize apenas o seu `.env`.
- Se `mvn exec:java` falhar com erro parecido com `/snap/core20/.../libpthread.so.0`, o problema tende a ser do ambiente Snap do terminal, nao do projeto. Prefira um terminal normal do sistema ou teste:

```bash
env -u LD_LIBRARY_PATH -u LD_PRELOAD mvn exec:java
```
