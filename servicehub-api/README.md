# ServiceHub API

API REST (Spring Boot) que conecta quem precisa de um serviço a quem o oferece: o **prestador publica** um
serviço, um usuário **contrata** (abre uma ordem), o prestador **aceita, inicia e conclui**, e quem contratou
**avalia** (nota de 1 a 5). Projeto de Engenharia de Software / Desenvolvimento de API Back-End
(UniEVANGÉLICA 2026/2), construído conforme o **Mapa de Construção v1.2**.

## Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · Flyway · Spring Security + JWT (HS256) · BCrypt · springdoc
(Swagger) · Spring GraphQL · H2 (padrão) e PostgreSQL 16 · JUnit 5, Mockito, Testcontainers · Docker Compose.

## Como rodar

### Opção 1: Docker Compose (API + PostgreSQL, com volume persistente)

```bash
cp .env.example .env        # edite JWT_SECRET, ADMIN_* e as senhas
docker compose up -d --build
```

API em **http://localhost:8090** · Swagger UI em **/docs** · OpenAPI em **/api-docs** · health em **/actuator/health**.

### Opção 2: local, sem instalar banco (H2 em memória)

```bash
export JWT_SECRET="um-segredo-com-no-minimo-32-bytes-0123456789"
export ADMIN_EMAIL=admin@servicehub.local ADMIN_PASSWORD=Admin1234   # opcional: cria o primeiro ADMIN
mvn spring-boot:run
```

Sem `JWT_SECRET` (mínimo 32 bytes) a aplicação **não sobe**. Para PostgreSQL local:
`SPRING_PROFILES_ACTIVE=postgres` (usa `DB_URL`, `DB_USER`, `DB_PASSWORD`).

### Variáveis de ambiente

| Variável | Para quê | Padrão |
|---|---|---|
| `JWT_SECRET` | Segredo do JWT (mín. 32 bytes). **Obrigatória.** | — |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | Criam o primeiro ADMIN no startup | vazio (não cria) |
| `CORS_ALLOWED_ORIGINS` | Origens liberadas (vírgula) | `http://localhost:5173` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Banco (profile `postgres`) | H2 em memória |
| `SERVER_PORT` | Porta | `8090` |

## Como testar

```bash
mvn verify
```

- **Unitários (Mockito):** um teste por regra RN01–RN13; o `@DisplayName` começa com o código da regra.
- **Fatia web (`@WebMvcTest`):** formato do ProblemDetail, validações, mass assignment.
- **Integração (MockMvc + H2):** fluxo completo do Mapa, 401 em todas as rotas protegidas, CORS, `X-Request-Id`, GraphQL.
- **PostgreSQL real (Testcontainers):** migrações do zero + `ddl-auto=validate`. Ignorado se não houver Docker.

## Fluxo completo (curl)

```bash
API=http://localhost:8090/api/v1
curl -s -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Maria Silva","email":"maria@email.com","password":"Senha123","role":"PROVIDER"}'
TOKEN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"maria@email.com","password":"Senha123"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
curl -s -X POST $API/services -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Instalação elétrica residencial","price":150.00,"category":"ELETRICA"}'
```

## Endpoints (prefixo `/api/v1`)

| Grupo | Rotas | Acesso |
|---|---|---|
| Autenticação | `POST /auth/register`, `POST /auth/login`, `GET /auth/me` | público, público, autenticado |
| Usuários | `GET /users`, `GET/PUT/DELETE /users/{id}`, `PATCH /users/{id}/role`, `PATCH /users/{id}/active` | ADMIN ou o próprio |
| Serviços | `GET /services`, `GET /services/{id}`, `GET /services/{id}/reviews` | público |
| | `POST /services` · `PUT/DELETE /services/{id}` | PROVIDER · dono ou ADMIN |
| Ordens | `POST /service-requests`, `GET /service-requests`, `GET /service-requests/{id}`, `PATCH /service-requests/{id}/status`, `POST /service-requests/{id}/reviews` | autenticado (regras por papel/dono) |
| Avaliações e operação | `GET /reviews/{id}`, `GET /info` · `/actuator/health` | público |
| Métricas | `/actuator/metrics`, `/actuator/prometheus` | ADMIN |
| GraphQL | `POST /graphql` (consultas `services` e `service`, somente leitura) | público |

Convenções: JSON `camelCase`; datas ISO-8601 sem fuso (`2026-11-05T14:30:00`); listagens paginadas
(`?page=0&size=20&sort=createdAt,desc`, máximo 100); erros sempre em `application/problem+json`
(`urn:servicehub:problem:*`); toda resposta traz `X-Request-Id`.

```graphql
{ services(minPrice: 20, size: 5) { totalElements content { id title price category providerName } } }
```

## Máquina de estados da ordem

`PENDING → ACCEPTED → IN_PROGRESS → COMPLETED` · `PENDING/ACCEPTED → CANCELLED` (cliente ou prestador).
Aceitar, iniciar e concluir: só o prestador dono. `COMPLETED` e `CANCELLED` são terminais. Transição fora da
tabela: `409 invalid-transition`; quem não participa da ordem recebe `403` antes de qualquer análise.

## Arquitetura

```
Controller (rotas, DTOs, @Valid)  →  Service (regras RN01–RN13)  →  Repository (Spring Data JPA)  →  PostgreSQL/H2
```

`controller/` · `service/` · `repository/` · `model/` (entidades) · `dto/` · `security/` (JWT, filtro) ·
`config/` (Security, OpenAPI, Jackson, ADMIN inicial) · `exception/` (ProblemDetail) · `graphql/` · `web/` (X-Request-Id).

## Documentação do processo

- `docs/diagnostico.md` — etapa 1: o que atendia, o que faltava, o que contrariava o Mapa.
- `docs/decisoes.md` — decisões técnicas (problema → decisão → consequência).
- `docs/relatorio-checkpoint.md` — relatório de entrega.
- `CLAUDE.md` — contexto e convenções do projeto.
