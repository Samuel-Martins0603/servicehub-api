# ServiceHub API — Checkpoint

API REST para gestão de serviços, desenvolvida com Spring Boot como parte do
Checkpoint da disciplina de Engenharia de Software.

## Stack

- Java 17
- Spring Boot 3.3 (Spring Web, Spring Data JPA, Bean Validation)
- H2 (banco em memória, padrão) com perfil pronto para PostgreSQL
- SpringDoc OpenAPI (Swagger UI)

## Como rodar

```bash
./mvnw spring-boot:run
```

(Se não tiver o wrapper gerado, rode `mvn spring-boot:run` com o Maven
instalado, ou importe o projeto no IntelliJ/Eclipse/VS Code e rode a classe
`ServicehubApiApplication`.)

A aplicação sobe em `http://localhost:8080` usando H2 em memória — nenhuma
instalação de banco é necessária para o checkpoint.

Para usar PostgreSQL de verdade:

```bash
docker run --name servicehub-db -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=servicehub -p 5432:5432 -d postgres:16

./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Endpoints

Documentação interativa: **http://localhost:8080/swagger-ui.html**

| Método | Rota                | Descrição                          | Status de sucesso |
|--------|----------------------|-------------------------------------|--------------------|
| GET    | `/api/servicos`      | Lista todos os serviços             | 200 OK             |
| GET    | `/api/servicos/{id}` | Busca um serviço pelo id            | 200 OK / 404       |
| POST   | `/api/servicos`      | Cria um novo serviço                | 201 Created / 400  |
| PUT    | `/api/servicos/{id}` | Atualiza um serviço (substituição)  | 200 OK / 400 / 404 |
| DELETE | `/api/servicos/{id}` | Remove um serviço                   | 204 No Content / 404 |

### Exemplo de payload (POST/PUT)

```json
{
  "nome": "Corte de cabelo",
  "descricao": "Corte tradicional com máquina e tesoura",
  "preco": 45.90,
  "duracaoMinutos": 30,
  "ativo": true
}
```

## Arquitetura em camadas

```
Controller  ->  Service  ->  Repository  ->  Banco de dados
(HTTP/JSON)     (regras)     (JpaRepository)
```

- `controller/` — recebe a requisição, valida entrada (`@Valid`) e delega.
- `service/` — regra de negócio; orquestra o repositório.
- `repository/` — interface `JpaRepository`, sem SQL escrito à mão.
- `model/` — entidade JPA (mapeamento objeto-relacional).
- `dto/` — contrato público da API (request/response), separado da entidade.
- `exception/` — exceção de negócio + `@RestControllerAdvice` central.
- `config/` — metadados do OpenAPI.

## Roteiro sugerido para a apresentação (8–10 min)

1. **Contexto** (1–2 min): o que é o ServiceHub API e o que este checkpoint cobre.
2. **Código** (3 min): mostrar as camadas (Controller → Service → Repository)
   e reforçar a separação de responsabilidades.
3. **Swagger** (2 min): abrir `/swagger-ui.html`, mostrar os 5 endpoints,
   summaries/descriptions e os exemplos de payload.
4. **Chamadas ao vivo** (2–3 min): usar o próprio Swagger UI (botão "Try it
   out") ou `curl`/Postman para criar, listar, atualizar e excluir um
   serviço, mostrando os status codes corretos.

> Garanta que a aplicação já está rodando **antes** de começar a apresentar.
