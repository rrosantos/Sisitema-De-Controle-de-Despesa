# Instalacao e Execucao

## Requisitos

- Java 21
- Maven 3.8+
- MySQL 8

## Preparacao do banco

1. Crie o arquivo local de ambiente:

```bash
cp .env.example .env
```

2. Ajuste as credenciais no `.env`.

3. Execute o schema:

```bash
mysql -u root -p < database/schema.sql
```

4. Se necessario no Ubuntu:

```bash
sudo mysql < database/schema.sql
```

5. Valide o acesso do usuario da aplicacao:

```bash
mysql -u controle_app -p controle_despesas
```

## Execucao em desenvolvimento

```bash
mvn clean compile
mvn exec:java
```

## Geracao do jar executavel

```bash
mvn clean package
```

Arquivo gerado:

```text
target/sistema-controle-despesas-1.0.0-SNAPSHOT-app.jar
```

## Execucao do jar

Opcao 1:

```bash
./run.sh
```

Opcao 2:

```bash
java -jar target/sistema-controle-despesas-1.0.0-SNAPSHOT-app.jar
```

## Observacoes importantes

- O `.env` nao vai dentro do jar.
- O comando deve ser executado a partir de uma pasta que contenha o `.env`.
- Se quiser levar a aplicacao para outra pasta, copie tambem o `.env` local e o jar.

## Solucao de problemas

### Erro de autenticacao do MySQL

- Verifique se a senha do usuario `controle_app` no banco e a mesma do `.env`.
- Se o schema foi executado antes com outra senha, ajuste o usuario no MySQL ou atualize o `.env`.

### Erro GLIBC com Snap

Se a execucao falhar com mensagem parecida com:

```text
/usr/bin/java: symbol lookup error: /snap/core20/current/lib/x86_64-linux-gnu/libpthread.so.0: undefined symbol: __libc_pthread_init, version GLIBC_PRIVATE
```

o problema e do ambiente do terminal, normalmente relacionado a bibliotecas do Snap herdadas por IDEs como o VS Code instalado via Snap.

Tente:

```bash
env -u LD_LIBRARY_PATH -u LD_PRELOAD mvn exec:java
```

ou execute em um terminal externo do sistema.
