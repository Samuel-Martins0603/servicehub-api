# Relatório de Checkpoint — ServiceHub API

## 1. Identificação
- **Repositório:** _(colar aqui o link do GitHub depois do push)_
- **Último commit:** 27/10/2026

## 2. Estado das entregas

### Aula 8 · Segurança e validação — **pronto**
**Como comprovei:** Spring Security com `SecurityFilterChain` stateless (CSRF desabilitado, pois a API usa Bearer
token) protege todas as rotas fora da lista pública; existe um teste que percorre as rotas protegidas e confirma
**401 sem token**. Senhas só como hash **BCrypt** (RN02); a senha exige de 8 a 72 caracteres, ao menos 1 letra
maiúscula e 1 número (Bean Validation). Autorização por papel com `@EnableMethodSecurity` e `@PreAuthorize`, e
regras de dono verificadas no service com o usuário do token, o que evita **BOLA** (OWASP API1). Os DTOs de
entrada não têm `id`, `role`, `providerId`, `clientId` nem `status`, o que evita **mass assignment** (API3), e a
API só devolve DTOs de saída (sem senha nem hash). Textos livres passam por escape de HTML e as consultas são
parametrizadas. Foram implementadas as regras de negócio **RN01 a RN13** e o **versionamento por path `/ServiceHub/v1`**.
Erros seguem o padrão ProblemDetail (RFC 9457), sem stack trace.

### Aula 9 · JWT, papéis e endpoint alternativo — **pronto**
**Como comprovei:** autenticação por **JWT HS256** (24 h; claims `sub`, `uid`, `roles`, `iat`, `exp`), com segredo
em variável de ambiente (mínimo 32 bytes, senão a aplicação não sobe) e usuário conferido no banco a cada
requisição (usuário inativo recebe 401). Papéis **CLIENT, PROVIDER e ADMIN**, com **máquina de estados** da ordem
de serviço (PENDING → ACCEPTED → IN_PROGRESS → COMPLETED, ou CANCELLED). Endpoint alternativo com **GraphQL**
(`POST /graphql`), somente leitura do catálogo público, reaproveitando a mesma camada de serviço do REST.

### Aula 10 · Testes — **parcial**
**Como comprovei:** testes unitários com **Mockito** para as regras RN01 a RN13 (um teste por regra, com o código
no `@DisplayName`), teste de fatia web (`@WebMvcTest`), testes de integração com MockMvc cobrindo o fluxo completo
(cadastro → login → serviço → ordem → aceitar → iniciar → concluir → avaliar), 401 nas rotas protegidas, CORS e
GraphQL, e um teste com **PostgreSQL real via Testcontainers**. Falta medir cobertura e ver o pipeline do GitHub
Actions verde no último commit.

## 3. Decisão técnica e justificativa
Foco em **segurança e integridade dos dados**: segurança foi a prioridade do ciclo para proteger os dados dos
usuários e evitar falhas como BOLA e mass assignment. O banco passou a evoluir **só por migração Flyway**
(`ddl-auto=validate`), e o usuário do token é conferido no banco a cada requisição, para que desativar ou
rebaixar alguém valha na hora. Detalhes em `docs/decisoes.md`.

## 4. Dificuldade ou erro e como resolvemos
O checkpoint original era apenas um CRUD de `Servico` sem usuários, segurança nem testes; a distância até o Mapa
foi mapeada em `docs/diagnostico.md` e fechada por refatoração (sem reescrever). Em especial, o schema da seção 3
(colunas `TEXT`, enums, `SMALLINT`) precisou de ajustes para o `ddl-auto=validate` do Hibernate aceitar também o
H2 de desenvolvimento. Os testes foram a parte com menos tempo de polimento.

## 5. Próximos passos
- Rodar `mvn verify` e o pipeline do GitHub Actions até ficar verde, corrigindo o que aparecer.
- Medir cobertura de testes e ampliar os casos de borda da paginação e dos filtros.
- Conferir a lista de `ServiceCategory` com o `servicehub_schema.sql` do professor.
