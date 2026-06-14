# Sistema de Controle de Despesas Pessoais

Projeto base em Java 21 com Swing, Maven, MySQL, JDBC e `dotenv-java` para iniciar o Sistema de Controle de Despesas Pessoais.

## Tecnologias utilizadas

- Java 21
- Java Swing
- Maven
- MySQL
- JDBC
- dotenv-java
- JUnit 5

## Requisitos

- Java 21
- Maven
- MySQL

## Estrutura do projeto

```text
database/
src/
  main/
    java/
      br/com/controledespesas/
        app/
        config/
        controller/
        dao/
        database/
        model/
        service/
        util/
        view/
    resources/
  test/
    java/
      br/com/controledespesas/config/
```

- `app`: ponto de entrada da aplicacao.
- `config`: leitura e validacao das variaveis de ambiente.
- `database`: conexao JDBC e scripts SQL.
- `view`: interface Swing inicial.
- `controller`, `dao`, `model`, `service`, `util`: reservados para as proximas etapas.

## Configuracao do banco

1. Crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

2. Ajuste os valores do `.env` para o seu ambiente:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=controle_despesas
DB_USER=controle_app
DB_PASSWORD=troque_esta_senha
```

3. Execute o script inicial do banco:

```bash
mysql -u root -p < database/schema.sql
```

4. Confirme que a senha definida em `database/schema.sql` e a mesma informada em `.env`.

## Comandos

Compilar o projeto:

```bash
mvn clean compile
```

Executar os testes:

```bash
mvn test
```

Iniciar a aplicacao:

```bash
mvn exec:java
```

## Observacoes sobre o `.env`

O arquivo `.env` nao deve ser enviado ao GitHub, pois pode conter credenciais locais. O arquivo `.env.example` deve permanecer versionado para servir de modelo de configuracao.

## Solucao de problemas de conexao

- Verifique se o MySQL esta em execucao.
- Confira se `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` estao preenchidos corretamente.
- Garanta que a senha do usuario `controle_app` no MySQL seja a mesma do arquivo `.env`.
- Reexecute `database/schema.sql` caso o banco ou o usuario ainda nao existam.
- Confirme se esta usando Java 21 e Maven instalados no sistema.
