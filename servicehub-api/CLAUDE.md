# Contexto do projeto (para assistentes de IA e para a equipe)

**ServiceHub API** — Engenharia de Software / Desenvolvimento de API Back-End (UniEVANGÉLICA 2026/2).
Fonte da verdade: *Mapa de Construção v1.2* (contrato, regras RN01–RN13, segurança, critérios de aceite).

## Stack
Java 17, Spring Boot 3.3.4, Spring Data JPA, Flyway, Spring Security + JWT (jjwt), springdoc, Spring GraphQL,
H2 (padrão, modo PostgreSQL) e PostgreSQL 16 (profile `postgres` / Docker Compose). Testes: JUnit 5, Mockito,
`@WebMvcTest`, MockMvc, Testcontainers.

## Comandos
- Rodar: `export JWT_SECRET=<32+ bytes>; mvn spring-boot:run` (porta 8090; Swagger em `/docs`)
- Testes: `mvn verify` (os testes definem o próprio segredo; o de PostgreSQL real é ignorado sem Docker)
- Docker: `cp .env.example .env && docker compose up -d --build`

## Convenções
1. Refatorar, não reescrever. Cada commit compila e passa nos testes; mensagens `feat:`, `fix:`, `refactor:`, `test:`, `docs:`.
2. Banco só por migração `V{n+1}__descricao.sql`. Nunca editar migração aplicada. `ddl-auto=validate`.
3. Rotas sob `/ServiceHub/v1`; JSON camelCase; erros sempre em ProblemDetail (`urn:servicehub:problem:*`).
4. Controller só delega; regra de negócio no Service; entidade JPA nunca sai na API (DTO de saída) e DTO de
   entrada nunca tem `id`, `role`, `providerId`, `clientId` ou `status`.
5. Todo texto livre (`description`, `notes`, `comment`, `bio`) passa por `TextSanitizer`.
6. Cada regra RN tem teste cujo `@DisplayName` começa com o código (ex.: `RN08: ...`).
7. Nenhum segredo no Git (`.env` ignorado; `.env.example` versionado).

## Decisões
Ver `docs/decisoes.md` (problema → decisão → consequência) e `docs/diagnostico.md` (etapa 1).
