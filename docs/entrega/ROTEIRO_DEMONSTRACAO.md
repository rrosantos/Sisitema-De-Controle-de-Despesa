# Roteiro de Demonstracao

## Objetivo

Apresentar o fluxo principal do sistema, destacando autenticacao, isolamento por usuario, operacoes financeiras e dashboard.

## Sequencia sugerida

1. Mostrar o arquivo `.env.example` e explicar que o `.env` real nao e versionado.
2. Mostrar rapidamente `database/schema.sql` e destacar as tabelas por usuario.
3. Executar a aplicacao com `./run.sh` ou `mvn exec:java`.
4. Fazer login com um usuario existente.
5. Mostrar a tela principal e o dashboard financeiro.
6. Alternar o periodo do dashboard e destacar os cards de saldo, receitas, despesas e resultado.
7. Abrir `Categorias` e mostrar cadastro, edicao e ativacao/inativacao.
8. Abrir `Contas` e mostrar saldo inicial versus saldo atual calculado.
9. Abrir `Transacoes`, aplicar filtros e cadastrar ou editar uma receita e uma despesa.
10. Voltar ao dashboard para mostrar atualizacao do resumo.
11. Abrir `Cofrinhos`, registrar deposito ou retirada e explicar a regra de progresso da meta.
12. Fazer logout e mostrar retorno seguro para a tela de autenticacao.

## Pontos tecnicos para comentar

- Senhas com BCrypt.
- Uso de `PreparedStatement` e `try-with-resources`.
- Regras de negocio na camada `service`.
- Controle transacional em transacoes e movimentacoes de cofrinho.
- Uso de `BigDecimal` para valores financeiros.
- Carregamento assincrono na interface para nao bloquear a EDT.

## Plano B

Se a execucao pela IDE falhar por ambiente Snap, repetir a abertura em terminal externo com:

```bash
env -u LD_LIBRARY_PATH -u LD_PRELOAD mvn exec:java
```
