package br.com.servicehub.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguranca (Mapa, secao 10): 401 sem token, rotas publicas, papeis, CORS e rastreio por X-Request-Id. */
class SecurityIntegrationTest extends IntegrationTestSupport {

    @ParameterizedTest(name = "{0} {1} sem token responde 401")
    @CsvSource({
            "GET,/ServiceHub/v1/auth/me",
            "GET,/ServiceHub/v1/users",
            "GET,/ServiceHub/v1/users/1",
            "PUT,/ServiceHub/v1/users/1",
            "DELETE,/ServiceHub/v1/users/1",
            "PATCH,/ServiceHub/v1/users/1/role",
            "PATCH,/ServiceHub/v1/users/1/active",
            "POST,/ServiceHub/v1/services",
            "PUT,/ServiceHub/v1/services/1",
            "DELETE,/ServiceHub/v1/services/1",
            "POST,/ServiceHub/v1/service-requests",
            "GET,/ServiceHub/v1/service-requests",
            "GET,/ServiceHub/v1/service-requests/1",
            "PATCH,/ServiceHub/v1/service-requests/1/status",
            "POST,/ServiceHub/v1/service-requests/1/reviews",
            "GET,/actuator/metrics",
            "GET,/actuator/prometheus"
    })
    @DisplayName("Segurança: todas as rotas protegidas respondem 401 sem token")
    void protectedRoutesRequireToken(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @DisplayName("Segurança: token inválido, adulterado ou mal formado responde 401")
    void invalidTokensAreRejected() throws Exception {
        String valid = tokenFor("CLIENT");
        mockMvc.perform(get("/ServiceHub/v1/auth/me").header("Authorization", "Bearer lixo.lixo.lixo"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ServiceHub/v1/auth/me").header("Authorization", "Bearer " + valid + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ServiceHub/v1/auth/me").header("Authorization", valid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(get("/ServiceHub/v1/auth/me"), valid)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Segurança: rotas públicas funcionam sem token")
    void publicRoutesNeedNoToken() throws Exception {
        mockMvc.perform(get("/ServiceHub/v1/services")).andExpect(status().isOk());
        mockMvc.perform(get("/ServiceHub/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
        mockMvc.perform(post("/ServiceHub/v1/auth/login").contentType("application/json")
                        .content("{\"email\":\"ninguem@teste.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:unauthorized"));
    }

    @Test
    @DisplayName("Segurança: metrics só para ADMIN (CLIENT recebe 403)")
    void metricsAreAdminOnly() throws Exception {
        mockMvc.perform(authed(get("/actuator/metrics"), tokenFor("CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:forbidden"));
        mockMvc.perform(authed(get("/actuator/metrics"), adminToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Autorização por papel: CLIENT não publica serviço (403) e não lista usuários (403)")
    void roleBasedAuthorization() throws Exception {
        String client = tokenFor("CLIENT");
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Serviço", "price", "10.00", "category", "LIMPEZA")), client))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mockMvc.perform(authed(get("/ServiceHub/v1/users"), client)).andExpect(status().isForbidden());
        mockMvc.perform(authed(delete("/ServiceHub/v1/users/1"), client)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Segurança: o papel vale a partir do banco (mudança de papel vale no mesmo token)")
    void roleChangeAppliesToExistingToken() throws Exception {
        String email = uniqueEmail("promovido");
        long id = register(email, "CLIENT");
        String token = login(email, PASSWORD);
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Serviço novo", "price", "10.00", "category", "JARDINAGEM")), token))
                .andExpect(status().isForbidden());

        mockMvc.perform(authed(jsonBody(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/ServiceHub/v1/users/" + id + "/role"), Map.of("role", "PROVIDER")), adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Serviço novo", "price", "10.00", "category", "JARDINAGEM")), token))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CORS: libera http://localhost:5173 com os métodos e headers do Mapa; outras origens são recusadas")
    void corsAllowsTheTestBenchOrigin() throws Exception {
        MvcResult result = mockMvc.perform(options("/ServiceHub/v1/services")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andReturn();
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Methods")).contains("PATCH").contains("DELETE");
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Headers").toLowerCase())
                .contains("authorization").contains("content-type");

        mockMvc.perform(options("/ServiceHub/v1/services")
                        .header("Origin", "http://site-malicioso.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/ServiceHub/v1/services").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("Location")));
    }

    @Test
    @DisplayName("Rastreio: X-Request-Id válido é devolvido; ausente ou inválido gera um UUID")
    void requestIdIsEchoedOrGenerated() throws Exception {
        mockMvc.perform(get("/ServiceHub/v1/info").header("X-Request-Id", "minha-requisicao-123"))
                .andExpect(header().string("X-Request-Id", "minha-requisicao-123"));

        String generated = mockMvc.perform(get("/ServiceHub/v1/info")).andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(generated).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

        String replaced = mockMvc.perform(get("/ServiceHub/v1/info").header("X-Request-Id", "x"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(replaced).isNotEqualTo("x").hasSize(36);
    }

    @Test
    @DisplayName("Erros: rota inexistente e método não suportado respondem em ProblemDetail (nunca 500)")
    void unknownRouteAndWrongMethodAreProblemDetails() throws Exception {
        String token = tokenFor("CLIENT");
        mockMvc.perform(authed(get("/ServiceHub/v1/rota-que-nao-existe"), token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:not-found"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(authed(delete("/ServiceHub/v1/info"), token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        mockMvc.perform(authed(get("/ServiceHub/v1/services/abc"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:validation-error"));
    }

    @Test
    @DisplayName("Erros: recurso inexistente responde 404 not-found com instance preenchida")
    void notFoundHasInstance() throws Exception {
        mockMvc.perform(get("/ServiceHub/v1/services/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:not-found"))
                .andExpect(jsonPath("$.instance").value("/ServiceHub/v1/services/99999999"));
        mockMvc.perform(get("/ServiceHub/v1/reviews/99999999")).andExpect(status().isNotFound());
    }
}
