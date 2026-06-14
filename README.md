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

3. Execute o script do banco:

```bash
mysql -u root -p < database/schema.sql
```

Se o MySQL root estiver configurado para autenticacao via `sudo`, use:

```bash
sudo mysql < database/schema.sql
```

4. Confirme que a senha definida em `database/schema.sql` e a mesma informada em `.env`.

5. Teste o acesso com o usuario da aplicacao:

```bash
mysql -u controle_app -p controle_despesas
```

## Estrutura do banco de dados

O schema atual cria seis tabelas principais:

- `usuarios`: armazena nome, email, senha e status do usuario.
- `categorias`: separa categorias de receita e despesa por usuario.
- `contas`: registra as contas financeiras do usuario e o saldo inicial de cada uma.
- `transacoes`: concentra receitas e despesas com categoria, conta, valor, data e status.
- `cofrinhos`: representa metas financeiras do usuario.
- `movimentacoes_cofrinho`: registra depositos e retiradas de cada cofrinho.

Principais relacionamentos:

- `usuarios` 1:N `categorias`
- `usuarios` 1:N `contas`
- `usuarios` 1:N `transacoes`
- `usuarios` 1:N `cofrinhos`
- `usuarios` 1:N `movimentacoes_cofrinho`
- `categorias` 1:N `transacoes`
- `contas` 1:N `transacoes`
- `cofrinhos` 1:N `movimentacoes_cofrinho`

Regras importantes desta modelagem:

- Cada registro financeiro pertence a um usuario especifico.
- Valores monetarios usam `DECIMAL(12,2)`.
- O saldo atual das contas nao e armazenado de forma redundante; ele sera calculado futuramente a partir de `saldo_inicial` e das `transacoes`.
- O valor atual dos cofrinhos nao e armazenado diretamente; ele sera calculado pelas `movimentacoes_cofrinho`.
- Exclusoes de categorias e contas com transacoes vinculadas sao bloqueadas para preservar historico.

Observacao de desenvolvimento:

- O script usa `CREATE TABLE IF NOT EXISTS`, o que ajuda em bancos novos, mas nao altera tabelas antigas com estrutura diferente.
- Em evolucoes futuras, mudancas estruturais devem ser feitas por migracoes ou por recriacao consciente do banco de teste.

## Camada de acesso a dados

Nesta etapa, o projeto passou a contar com:

- Models Java para `usuarios`, `categorias`, `contas`, `transacoes`, `cofrinhos` e `movimentacoes_cofrinho`.
- Enums para todos os campos `ENUM` definidos no banco, com conversao centralizada entre Java e valor persistido.
- DAOs JDBC com operacoes de insercao, busca, listagem, atualizacao, exclusao e consultas agregadas.
- DTO `TransacaoFiltro` para filtros opcionais de listagem de transacoes.

Principios adotados:

- Valores financeiros usam exclusivamente `BigDecimal`.
- Datas usam `LocalDate` e `LocalDateTime`.
- Os DAOs utilizam `PreparedStatement` e `try-with-resources` em todas as operacoes.
- As buscas, alteracoes e exclusoes das entidades financeiras restringem o acesso por `usuario_id`.
- Os DAOs propagam `SQLException` em vez de esconder erros de persistencia.
- Senhas de usuario nao devem ser armazenadas em texto puro; o campo do model e `senhaHash`, e o DAO espera receber um valor ja preparado para armazenamento.

Consultas calculadas:

- O saldo atual das contas e calculado com base no `saldo_inicial` e nas `transacoes` efetivamente recebidas ou pagas.
- O valor atual dos cofrinhos e calculado pela soma de depositos menos retiradas em `movimentacoes_cofrinho`.
- O percentual de progresso do cofrinho e calculado em Java com `BigDecimal` e arredondamento explicito.

Limite desta etapa:

- As regras completas de negocio ainda nao ficam nos DAOs.
- Validacoes mais ricas, hash de senha, autenticacao, operacoes compostas e transacoes JDBC multi-etapas serao tratadas na futura camada de Services.

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
- Se `mysql -u root -p` falhar com erro de autenticacao no Ubuntu, tente `sudo mysql < database/schema.sql`.
- Confirme se esta usando Java 21 e Maven instalados no sistema.
