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
- `view`: telas Swing, paines de autenticacao, modulos financeiros e componentes reutilizaveis da interface.
- `dao`: acesso JDBC ao banco de dados.
- `model`: entidades e enums do dominio.
- `service`: regras de negocio, validacoes, autenticacao e coordenacao transacional.
- `util`: calculos e funcoes auxiliares reutilizaveis.
- `controller`: coordenacao entre eventos da interface, Services e navegacao da aplicacao.

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

Limite da camada DAO:

- As regras completas de negocio nao ficam concentradas nos DAOs.
- Validacoes, autenticacao e operacoes compostas foram deslocadas para a camada de Services, mantendo os DAOs focados em persistencia.

## Camada de servicos e regras de negocio

Nesta etapa, o projeto passou a ter uma camada de Services responsavel por aplicar validacoes de dominio antes de chegar ao banco e por coordenar operacoes compostas com JDBC.

Diferenca entre DAO e Service:

- `DAO`: executa SQL, faz mapeamento entre `ResultSet` e Models e preserva o isolamento financeiro por `usuario_id`.
- `Service`: valida campos obrigatorios, normaliza textos, decide regras de negocio e controla transacoes com `commit` e `rollback`.

Seguranca e autenticacao:

- O cadastro de usuarios utiliza BCrypt para gerar hash de senha com salt automatico.
- Senhas nunca sao armazenadas em texto puro e nao permanecem expostas no retorno dos Services.
- O e-mail e sempre normalizado com `trim()` e `toLowerCase(Locale.ROOT)`.
- O login usa mensagem generica para credenciais invalidas e bloqueia usuarios inativos.
- A sessao da aplicacao desktop fica na classe `SessaoUsuario`, sem singleton estatico e sem manter `senhaHash`.

Validacoes principais:

- `CategoriaService` impede categoria duplicada por nome e tipo no mesmo usuario.
- `ContaService` impede conta duplicada por nome no mesmo usuario e valida saldo inicial com `BigDecimal`.
- `TransacaoService` valida propriedade de categoria e conta por `id + usuario_id`, bloqueia entidades inativas em novas transacoes e exige compatibilidade entre tipo e status.
- Receitas aceitam apenas `PENDENTE`, `RECEBIDO` ou `CANCELADO`.
- Despesas aceitam apenas `PENDENTE`, `PAGO` ou `CANCELADO`.
- Transacoes historicas continuam consultaveis mesmo que categoria ou conta tenha sido inativada; a troca para uma nova entidade exige que ela esteja ativa.

Transacoes JDBC:

- Cadastros e alteracoes de transacoes usam a mesma `Connection` para validar categoria, validar conta e persistir a operacao.
- Depositos, retiradas e exclusoes de movimentacoes do cofrinho executam na mesma transacao JDBC.
- Em caso de falha, o Service faz `rollback` e preserva a excecao original.

Regras dos cofrinhos:

- Novos cofrinhos sempre iniciam com status `EM_ANDAMENTO`.
- O valor atual e o percentual de progresso continuam calculados, sem armazenamento redundante em tabela.
- Retiradas nao podem ultrapassar o valor atual do cofrinho.
- Cofrinho cancelado nao aceita novas movimentacoes.
- Depositos que atingem a meta mudam automaticamente o status para `CONCLUIDO`.
- Retiradas ou exclusoes que reduzem o valor abaixo da meta retornam o status para `EM_ANDAMENTO`, exceto quando o cofrinho estiver `CANCELADO`.

Cobertura de testes:

- Foram adicionados testes unitarios para hash de senha, autenticacao, cadastro de usuario e todos os Services.
- Os testes usam Mockito e nao dependem de MySQL ativo.

## Interface inicial e autenticacao

Nesta etapa, a aplicacao ganhou o fluxo inicial completo em Swing, cobrindo inicializacao, navegacao entre login e cadastro, sessao autenticada e retorno seguro ao login no logout.

Fluxo de inicializacao:

- `Main` aplica o look and feel nativo.
- A conexao com o banco e validada antes de abrir a interface.
- `ApplicationContext` monta manualmente as dependencias compartilhadas.
- `ApplicationController` coordena a troca entre a tela de autenticacao e a tela principal.

Componentes principais:

- `AuthFrame`: janela com `CardLayout` para alternar entre login e cadastro.
- `LoginPanel`: coleta e-mail e senha e encaminha eventos ao `LoginController`.
- `CadastroUsuarioPanel`: coleta nome, e-mail, senha e confirmacao e encaminha eventos ao `CadastroUsuarioController`.
- `MainFrame`: apresenta o usuario autenticado, a mensagem inicial e a navegacao entre os modulos financeiros liberados na etapa atual.

Controllers e navegacao:

- `LoginController` autentica em segundo plano, limpa a senha apos cada tentativa e abre a tela principal em caso de sucesso.
- `CadastroUsuarioController` executa o cadastro em segundo plano, retorna ao login e preenche o e-mail cadastrado quando possivel.
- `MainController` exige sessao valida, exibe os dados do usuario e trata o logout.
- `ApplicationController` evita janelas duplicadas e garante que apenas uma tela de autenticacao ou principal esteja aberta por vez.

Sessao e seguranca:

- A mesma instancia de `SessaoUsuario` e compartilhada pelo contexto e pelo `AutenticacaoService`.
- A sessao nao e estatica e nao armazena `senhaHash`.
- O login continua usando mensagem generica para credenciais invalidas.
- Os campos de senha da interface sao limpos apos uso, e os arrays retornados pela view sao apagados pelos controllers.

Execucao assincrona:

- Login e cadastro usam `SwingWorker` por meio de um executor assincrono pequeno e especifico para nao bloquear o Event Dispatch Thread.
- O estado visual dos botoes e restaurado ao final de cada operacao, inclusive em caso de erro.

Separacao de responsabilidades:

- `View`: renderiza componentes Swing e expõe apenas os dados e eventos necessarios.
- `Controller`: interpreta eventos da interface, chama Services e decide a navegacao.
- `Service`: aplica validacoes, autenticacao e regras de negocio.
- `DAO`: executa SQL e mapeia persistencia.

Limitacao atual da interface:

- A interface ja entrega login, categorias, contas, transacoes e cofrinhos na mesma janela principal.
- Graficos e relatorios continuam como evolucoes futuras.

## Gerenciamento de categorias e contas

Nesta etapa, a tela principal passou a funcionar como a janela definitiva da aplicacao apos o login.

Navegacao principal:

- `MainFrame` agora usa `CardLayout` para alternar o conteudo sem abrir novos `JFrame`.
- O menu lateral destaca o modulo ativo e mantem `Inicio`, `Transacoes`, `Categorias`, `Contas` e `Cofrinhos` acessiveis na mesma janela.

Categorias:

- O modulo de categorias possui listagem, cadastro, edicao, ativacao, inativacao e exclusao com confirmacao.
- Os filtros por tipo, status e pesquisa por nome sao aplicados em memoria sobre a lista carregada pelo `CategoriaService`.
- O `CategoriaController` usa a sessao autenticada, executa operacoes assincronas com o executor baseado em `SwingWorker` e nunca acessa DAO diretamente.

Contas:

- O modulo de contas possui CRUD, alteracao de status, confirmacao de exclusao e exibicao do saldo inicial e do saldo atual.
- O saldo inicial continua persistido na tabela `contas`, enquanto o saldo atual e calculado pelo `ContaService` a partir de `saldo_inicial` e `transacoes`.
- A listagem formata valores em Real brasileiro com `BigDecimal` e uma utilitaria `MoneyFormatter`, sem uso de `double`.

Comportamento dos formularios:

- Os dialogos de categorias e contas agora permanecem abertos quando o `Service` retorna erro de validacao ou regra de negocio.
- Os dados preenchidos continuam preservados, a mensagem aparece no proprio formulario e o fechamento acontece somente apos sucesso.

Separacao de responsabilidades:

- `View`: renderiza componentes Swing, formularios modais, estados vazios e indicadores de carregamento.
- `Controller`: interpreta eventos, consulta a sessao, chama Services, trata mensagens e coordena a navegacao.
- `Service`: aplica regras de negocio, validacoes e protecoes por `usuario_id`.
- `DAO`: executa SQL e permanece isolado da interface grafica.

Pendencias intencionais:

- dashboard financeiro, graficos e relatorios.

## Gerenciamento de transacoes

Nesta etapa, o modulo `Transacoes` foi ativado no menu principal e no card inicial, usando o mesmo `CardLayout` da `MainFrame` sem recriar paines ou abrir novas janelas.

Listagem e navegacao:

- O `TransacaoController` carrega categorias, contas, transacoes e resumo financeiro com o mesmo usuario obtido de `SessaoUsuario`.
- O painel lista receitas e despesas com datas em `dd/MM/yyyy`, valores em Real brasileiro e nomes resolvidos de categoria e conta.
- `TransacaoTableModel` recebe apenas dados ja resolvidos e nao consulta banco por linha, por renderer nem por acao da tabela.

Cadastro, edicao e exclusao:

- O formulario modal reutiliza `MoneyFormatter` e `DateFormatter`, trabalha com `BigDecimal` e `LocalDate` e inicia novas transacoes com a data atual.
- Receitas aceitam apenas `PENDENTE`, `RECEBIDO` e `CANCELADO`; despesas aceitam apenas `PENDENTE`, `PAGO` e `CANCELADO`.
- Novos cadastros aceitam somente categorias e contas ativas; na edicao, o registro historico pode manter categoria ou conta inativa ja vinculada.
- Erros de validacao e regra de negocio permanecem dentro do dialogo, com os campos preservados, e o fechamento ocorre somente quando o `Service` confirma sucesso.
- A exclusao exige confirmacao, preserva os filtros atuais e recarrega lista e resumo apos a operacao.

Filtros e resumo:

- Os filtros cobrem data inicial, data final, tipo, status, categoria, conta e descricao, sempre convertidos para `TransacaoFiltro`.
- O resumo exibe `Receitas recebidas`, `Despesas pagas` e `Saldo do periodo`, calculados pelo `TransacaoService` sem somas manuais em Java.
- Os cards acompanham apenas o intervalo de datas informado; filtros por texto, categoria, conta ou status nao alteram os totais quando o calculo do `Service` considera somente o periodo.

Arquitetura e execucao:

- O modulo segue a separacao MVC ja adotada: a View apenas coleta dados e renderiza estados, o Controller coordena fluxo e os Services concentram as regras.
- Todas as operacoes de carregar, filtrar, cadastrar, atualizar e excluir usam o `AsyncTaskExecutor` para nao bloquear o EDT.
- O isolamento por usuario continua garantido por `usuario_id`, sem receber `usuarioId` da interface e sem acesso direto a DAO fora dos Services.
- As operacoes de cofrinho continuam independentes deste modulo e nao geram transacoes financeiras automaticamente.

## Gerenciamento de cofrinhos

Nesta etapa, o modulo `Cofrinhos` foi ativado no menu principal e no card inicial, usando o mesmo `CardLayout` da `MainFrame` sem recriar paineis, controllers ou a janela principal.

Funcionalidades entregues:

- Cadastro, edicao, cancelamento, reativacao e exclusao de cofrinhos com confirmacoes apropriadas.
- Depositos, retiradas, historico de movimentacoes e exclusao de movimentacoes dentro do proprio modulo.
- Resumo geral com `Total guardado`, metas em andamento, concluidas e canceladas, sempre calculado sobre a lista atualmente visivel.
- Filtros por nome, status e prazo, aplicados em memoria apos o carregamento dos resumos.

Regras e comportamento:

- O valor atual do cofrinho continua sendo calculado por `movimentacoes_cofrinho`, sem armazenamento redundante.
- Ao editar a meta ou reativar um cofrinho cancelado, o `CofrinhoService` recalcula o status dentro da mesma transacao JDBC.
- Depositos que atingem a meta mudam o status para `CONCLUIDO`; retiradas ou exclusoes de movimentacao podem retornar para `EM_ANDAMENTO`.
- Formularios e dialogs permanecem abertos em caso de erro de validacao, regra de negocio ou erro tecnico, e fecham apenas quando a operacao conclui com sucesso.

Importante sobre integracao com contas:

- Nesta versao, as movimentacoes dos cofrinhos nao sao vinculadas automaticamente a contas nem a transacoes financeiras.
- Depositar em um cofrinho nao reduz saldo de conta.
- Retirar de um cofrinho nao cria receita.
- Qualquer integracao futura entre contas e cofrinhos exigira uma decisao de modelagem e ficou fora do escopo desta etapa.

## Dashboard financeiro

Nesta etapa, o antigo painel `Inicio` foi refatorado para funcionar como um dashboard real do usuario autenticado, reaproveitando a arquitetura MVC, o `CardLayout` da tela principal e a execucao assincrona ja existente no projeto.

Periodo e atualizacao:

- O periodo padrao do dashboard vai do primeiro dia do mes atual ate a data atual.
- O filtro aceita `data inicial` e `data final` opcionais, com validacao de `data inicial <= data final`.
- Foram adicionados atalhos para `Mes atual`, `Mes anterior`, `Ultimos 30 dias`, `Este ano` e `Limpar periodo`.
- O carregamento acontece em segundo plano com `AsyncTaskExecutor`, sem bloquear o EDT.
- Depois de alteracoes bem-sucedidas em `Transacoes`, `Contas` e `Cofrinhos`, o dashboard e apenas marcado como desatualizado; o recarregamento acontece ao voltar para `Inicio` ou ao usar o botao `Atualizar`.

Indicadores principais:

- `Saldo total das contas`: soma os saldos atuais de todas as contas do usuario, incluindo contas ativas e inativas.
- `Receitas recebidas`: considera somente `tipo = RECEITA` e `status = RECEBIDO` dentro do periodo.
- `Despesas pagas`: considera somente `tipo = DESPESA` e `status = PAGO` dentro do periodo.
- `Resultado do periodo`: calcula `receitas recebidas - despesas pagas`, sempre com `BigDecimal`.
- O dashboard tambem informa quantas contas estao ativas e quantas transacoes seguem pendentes.

Seções do dashboard:

- `Resumo das contas`: mostra ate cinco contas com nome, tipo, status e saldo atual, com atalho para o modulo completo de contas.
- `Despesas por categoria`: mostra ate cinco categorias em barras horizontais simples, sem bibliotecas externas de grafico, com valor e percentual calculados.
- `Transacoes recentes`: exibe uma tabela compacta com ate cinco registros do periodo, ordenados por data e `id` decrescentes.
- `Cofrinhos e metas`: mostra metas priorizando as que estao em andamento e com prazo mais proximo, incluindo percentual, status e indicacao textual de atraso.

Consultas e isolamento:

- O dashboard usa `DashboardDAO` para consultas agregadas especificas, evitando N+1, consultas por linha e somas financeiras em memoria.
- Todas as consultas continuam restritas por `usuario_id`.
- A View nao acessa `Service` nem `DAO`, e o `Controller` do dashboard nao recebe `usuarioId` da interface.
- O `DashboardService` apenas valida dados, normaliza valores nulos, calcula percentuais e monta DTOs imutaveis.

Observacao importante:

- Os valores dos cofrinhos sao controlados independentemente das contas. Portanto, o total guardado nas metas nao e descontado automaticamente do saldo das contas.

Para iniciar a aplicacao:

```bash
mvn exec:java
```

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
