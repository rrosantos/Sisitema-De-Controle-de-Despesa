# Checklist de Entrega

## Banco e ambiente

- [ ] `.env` local configurado e fora do versionamento.
- [ ] `.env.example` atualizado com placeholders seguros.
- [ ] `database/schema.sql` executado com sucesso no MySQL.
- [ ] Login com `controle_app` validado no banco.

## Qualidade tecnica

- [ ] Isolamento por `usuario_id` revisado nas operacoes financeiras.
- [ ] Valores monetarios usando `BigDecimal`.
- [ ] `PreparedStatement` e `try-with-resources` usados nos DAOs.
- [ ] Fluxos transacionais com `commit` e `rollback` revisados.
- [ ] Senhas armazenadas como hash BCrypt.

## Interface e uso

- [ ] Login e cadastro funcionando.
- [ ] Modulos `Categorias`, `Contas`, `Transacoes` e `Cofrinhos` acessiveis pela tela principal.
- [ ] Dashboard carregando resumo financeiro por periodo.
- [ ] Logout retornando para a autenticacao.

## Build e testes

- [ ] `mvn clean test`
- [ ] `mvn clean compile`
- [ ] `mvn clean package`
- [ ] `mvn -DskipTests package`
- [ ] Jar executavel gerado em `target/sistema-controle-despesas-1.0.0-SNAPSHOT-app.jar`

## Entrega

- [ ] `README.md` revisado.
- [ ] `docs/entrega/INSTALACAO.md` revisado.
- [ ] `docs/entrega/ROTEIRO_DEMONSTRACAO.md` revisado.
- [ ] `docs/entrega/CHECKLIST_ENTREGA.md` revisado.
- [ ] Nenhuma credencial real adicionada ao Git.
