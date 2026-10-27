# Decisões técnicas (problema → decisão → consequência)

## 1. Segurança como prioridade do ciclo
- **Problema:** a API do checkpoint era totalmente aberta; qualquer pessoa criava, alterava e apagava dados.
- **Decisão:** Spring Security com `SecurityFilterChain` stateless, JWT HS256 de 24 h, BCrypt, papéis com
  `@PreAuthorize` e regras de dono verificadas no **service** com o usuário do token (nunca com id do corpo).
- **Consequência:** BOLA (OWASP API1) e mass assignment (API3) são tratados por construção: os DTOs de entrada
  não têm `id`, `role`, `providerId`, `clientId` nem `status`. O custo é um `findById` do usuário a cada requisição.

## 2. O usuário do token é conferido no banco a cada requisição
- **Problema:** um JWT é válido até expirar; sem checagem, um usuário desativado (RN11) ou rebaixado continuaria
  com acesso por até 24 h.
- **Decisão:** o `JwtAuthenticationFilter` valida a assinatura, lê `uid`, carrega o usuário e só autentica se
  estiver ativo. O papel usado é o do banco, não o da claim `roles`.
- **Consequência:** desativar ou mudar o papel vale imediatamente (401/403). Uma consulta extra por requisição.

## 3. Banco evolui só por migração; V3 descarta a tabela `servicos` do checkpoint
- **Problema:** `ddl-auto=create-drop` recriava o schema a cada start; o modelo da seção 3 é incompatível com
  `servicos` (sem prestador nem categoria, preço sem restrição).
- **Decisão:** `V1` registra a tabela original como baseline, `V2` e `V3` criam o schema final e a `V3` remove
  `servicos`. O Hibernate só **valida** (`ddl-auto=validate`).
- **Consequência:** nenhuma migração antiga foi editada. Os dados do checkpoint eram de demonstração (H2 em
  memória), então descartá-los não perde informação real. Em um banco com dados, seria preciso migrá-los antes.

## 4. `409 Conflict` para regra de negócio e `403` antes de avaliar a transição
- **Problema:** distinguir "dado mal formado", "sem permissão" e "regra violada" para o cliente da API.
- **Decisão:** 400 = formato/validação; 401 = identidade; 403 = papel ou dono errado; **409 = requisição válida que
  viola uma regra** (e-mail repetido, transição inválida, ordem não concluída, ordem já avaliada, ordens ativas).
  Em `PATCH /status`, quem não participa da ordem recebe 403 antes de qualquer análise da transição; depois,
  transição fora da tabela é 409 e, por fim, transição válida sem permissão é 403.
- **Consequência:** a mensagem do 409 (`detail`) explica a recusa ("Não é possível mudar de COMPLETED para PENDING.").

## 5. Colunas longas: `TEXT` no PostgreSQL e `VARCHAR(4000)` no H2 via placeholder Flyway
- **Problema:** o Mapa pede `TEXT`; no H2, `TEXT` vira `CLOB` e o `ddl-auto=validate` do Hibernate recusa.
- **Decisão:** as migrações usam o placeholder `${textType}` (`TEXT` no profile `postgres`, `VARCHAR(4000)` no H2).
  Enums usam `@Enumerated(STRING)` com `@JdbcTypeCode(VARCHAR)`; avaliação usa `short` (SMALLINT).
- **Consequência:** o schema do PostgreSQL fica idêntico ao do Mapa e o H2 serve para desenvolvimento rápido.

## 6. GraphQL como endpoint alternativo somente de leitura
- **Problema:** a Aula 9 pede um endpoint alternativo, mas duplicar regras abriria brecha de inconsistência.
- **Decisão:** `POST /graphql` expõe só consultas ao catálogo público e reaproveita `ServiceItemService`, então
  RN12 (inativo some) e a paginação máxima de 100 valem igual ao REST.
- **Consequência:** sem mutations, a superfície de ataque do GraphQL é mínima e não exige token.
