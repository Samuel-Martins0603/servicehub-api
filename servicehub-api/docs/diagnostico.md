# Diagnóstico (Etapa 1 do roteiro do Mapa de Construção)

Comparação entre o **Checkpoint original** (commit `chore: importa checkpoint original`) e o **alvo** do
Mapa de Construção v1.2. Esta é a lista de tarefas que guiou os commits seguintes.

## O que já atendia

| Item | Situação no checkpoint |
|---|---|
| Arquitetura em camadas Controller → Service → Repository | Atendia: mantida. |
| DTOs de entrada e saída (entidade nunca exposta) | Atendia: mantido o padrão (records). |
| Bean Validation com `@Valid` | Atendia: ampliado para as regras da seção 8. |
| `@RestControllerAdvice` central | Atendia: reescrito para ProblemDetail. |
| `201 Created` com `Location`, `204` na exclusão | Atendia: mantido. |
| Swagger/OpenAPI (springdoc) | Atendia: movido para `/docs` e `/api-docs`. |

## O que faltava

| Item do Mapa | Situação no checkpoint | Resolvido em |
|---|---|---|
| Domínio: usuários, serviços, ordens, avaliações | Só existia `Servico` (nome, duração) | Entidades `User`, `ServiceItem`, `ServiceRequest`, `Review` |
| Schema da seção 3 por migração Flyway | `ddl-auto=create-drop`, sem migrações | `V1` baseline, `V2` users, `V3` services/orders/reviews |
| `ddl-auto=validate` | `create-drop` | `application.properties` |
| Exclusão lógica, `ON DELETE RESTRICT`, `CHECK`s | Inexistente | Migrações V2/V3 |
| Prefixo `/api/v1` | Rotas `/api/servicos` | Todos os controllers |
| Paginação (padrão 20, máx. 100) | `GET` devolvia lista inteira | `PagedModel` + `Pageable` |
| Erros em ProblemDetail (RFC 9457) com `type`, `instance`, `timestamp` | `ApiErrorResponse` próprio | `GlobalExceptionHandler` + `Problems` |
| Header `X-Request-Id` | Inexistente | `RequestIdFilter` |
| Spring Security + JWT (HS256, 24h) + BCrypt | **Nenhuma segurança** | pacote `security` + `SecurityConfig` |
| Papéis CLIENT / PROVIDER / ADMIN, `@PreAuthorize`, regras de dono (BOLA) | Inexistente | controllers + services |
| Anti mass assignment / excessive data exposure | DTOs já separados, mas sem usuários | DTOs sem `id`, `role`, `providerId`, `clientId`, `status` |
| Sanitização de texto livre | Inexistente | `TextSanitizer` |
| CORS para `http://localhost:5173` | Inexistente | `SecurityConfig` |
| ADMIN inicial por variável de ambiente | Inexistente | `AdminInitializer` |
| Máquina de estados da ordem (RN08) | Inexistente | `RequestStatus` + `OrderService` |
| Regras RN01 a RN13 | Inexistentes | `AuthService`, `UserService`, `ServiceItemService`, `OrderService`, `ReviewService` |
| Endpoint alternativo GraphQL (Aula 9) | Inexistente | `graphql/schema.graphqls` + `ServiceGraphQlController` |
| Testes: um por regra RN, `@WebMvcTest`, integração com PostgreSQL real, 401 sem token | 1 teste de contexto | pasta `src/test` |
| Actuator (health público; metrics/prometheus ADMIN) | Inexistente | `SecurityConfig` + `application.properties` |
| Dockerfile multi-stage, Compose com healthcheck, `.env`, CI | Inexistente | `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml` |

## O que contrariava o Mapa

| Item | Problema | Correção |
|---|---|---|
| Rotas em português (`/api/servicos`) e campos `nome`, `preco`, `duracaoMinutos` | Contrato da seção 6/7 usa `/services`, `title`, `price`, `category` | Contrato reescrito |
| `ddl-auto=create-drop` | Banco não evolui por migração | `validate` + Flyway |
| H2 `jdbc:h2:mem` sem Postgres como alvo | Alvo é PostgreSQL via Compose | H2 em modo PostgreSQL por padrão; profile `postgres` |
| Console H2 habilitado | Abre superfície de ataque sem necessidade | Removido |
| Porta 8080 | Banca usa 8090 | `server.port=8090` |
| Tabela `servicos` sem prestador | Não cabe no modelo da seção 3 | Substituída por `services` (migração V3) |

## Pendências conhecidas

- A lista dos **21 valores de `ServiceCategory`** foi montada pela equipe; precisa ser conferida com o arquivo
  `servicehub_schema.sql` do professor (Enum Java, CHECK da V3 e `schema.graphqls` devem ficar idênticos).
- O repositório ainda precisa ser publicado no GitHub e o pipeline precisa rodar verde no último commit.
