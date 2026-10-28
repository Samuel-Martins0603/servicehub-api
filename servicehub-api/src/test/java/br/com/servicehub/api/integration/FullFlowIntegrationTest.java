package br.com.servicehub.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceite do Mapa (secao 12): fluxo completo cadastro -> login -> servico -> ordem ->
 * aceitar -> iniciar -> concluir -> avaliar, mais as regras RN01-RN13 vistas pela API HTTP real.
 */
class FullFlowIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("Fluxo completo: prestador publica, cliente contrata, prestador executa, cliente avalia (RN07, RN08, RN09)")
    void completeJourney() throws Exception {
        String providerEmail = uniqueEmail("prestador");
        String clientEmail = uniqueEmail("cliente");
        register(providerEmail, "PROVIDER");
        register(clientEmail, "CLIENT");
        String provider = login(providerEmail, PASSWORD);
        String client = login(clientEmail, PASSWORD);

        // serviço publicado: 201 + Location + prestador vem do token (RN04)
        MvcResult created = mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Instalação elétrica residencial", "description", "Tomadas e fiação",
                                "price", "150.00", "category", "ELETRICA")), provider))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/ServiceHub/v1/services/")))
                .andExpect(jsonPath("$.providerId").value(meId(provider)))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long serviceId = read(created, "$.id");

        // ordem nasce PENDING com preço congelado
        long orderId = createOrder(client, serviceId);
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests/" + orderId), client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(150.00))
                .andExpect(jsonPath("$.clientId").value(meId(client)));

        // RN07: o prestador reajusta o preço e a ordem de hoje não muda
        updateService(provider, serviceId, "999.00", null, 200);
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests/" + orderId), client))
                .andExpect(jsonPath("$.totalPrice").value(150.00));

        // RN08: o cliente não aceita (403); o prestador aceita, inicia, conclui
        changeStatus(client, orderId, "ACCEPTED", 403);
        changeStatus(provider, orderId, "ACCEPTED", 200);
        changeStatus(provider, orderId, "IN_PROGRESS", 200);
        // RN09: ainda não concluída: 409
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests/" + orderId + "/reviews"),
                        Map.of("rating", 5)), client))
                .andExpect(status().isConflict());
        changeStatus(provider, orderId, "COMPLETED", 200);

        // transição inválida: 409 com ProblemDetail explicando
        mockMvc.perform(authed(jsonBody(patch("/ServiceHub/v1/service-requests/" + orderId + "/status"),
                        Map.of("status", "PENDING")), provider))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:invalid-transition"))
                .andExpect(jsonPath("$.detail").value("Não é possível mudar de COMPLETED para PENDING."));

        // RN09: avaliar (só o cliente, uma vez)
        MvcResult review = mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests/" + orderId + "/reviews"),
                        Map.of("rating", 5, "comment", "Pontual e caprichosa.")), client))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/ServiceHub/v1/reviews/")))
                .andExpect(jsonPath("$.rating").value(5))
                .andReturn();
        long reviewId = read(review, "$.id");
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests/" + orderId + "/reviews"),
                        Map.of("rating", 4)), client))
                .andExpect(status().isConflict());
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests/" + orderId + "/reviews"),
                        Map.of("rating", 4)), provider))
                .andExpect(status().isForbidden());

        // avaliações são públicas (sem token)
        mockMvc.perform(get("/ServiceHub/v1/services/" + serviceId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(get("/ServiceHub/v1/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewerName").exists());
    }

    @Test
    @DisplayName("RN01: e-mail repetido (sem diferenciar maiúsculas) responde 409 email-already-exists")
    void rn01_duplicateEmailCaseInsensitive() throws Exception {
        String email = uniqueEmail("maria");
        register(email, "CLIENT");

        mockMvc.perform(jsonBody(post("/ServiceHub/v1/auth/register"),
                        Map.of("fullName", "Outra Maria", "email", email.toUpperCase(), "password", PASSWORD, "role", "CLIENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:servicehub:problem:email-already-exists"));
    }

    @Test
    @DisplayName("RN02: a resposta nunca traz senha nem hash")
    void rn02_noPasswordInAnyResponse() throws Exception {
        String email = uniqueEmail("sem-senha");
        MvcResult result = mockMvc.perform(jsonBody(post("/ServiceHub/v1/auth/register"),
                        Map.of("fullName", "Usuario Teste", "email", email, "password", PASSWORD, "role", "CLIENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("$2a$").doesNotContain(PASSWORD);

        String token = login(email, PASSWORD);
        mockMvc.perform(authed(get("/ServiceHub/v1/auth/me"), token))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("RN03: cadastro como ADMIN responde 400; só o ADMIN altera papéis")
    void rn03_roles() throws Exception {
        mockMvc.perform(jsonBody(post("/ServiceHub/v1/auth/register"),
                        Map.of("fullName", "Eve Hacker", "email", uniqueEmail("eve"), "password", PASSWORD, "role", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='role')]").exists());

        String clientToken = tokenFor("CLIENT");
        long clientId = meId(clientToken);
        // o próprio usuário tenta virar ADMIN: 403
        mockMvc.perform(authed(jsonBody(patch("/ServiceHub/v1/users/" + clientId + "/role"), Map.of("role", "ADMIN")), clientToken))
                .andExpect(status().isForbidden());
        // o ADMIN altera: 200
        mockMvc.perform(authed(jsonBody(patch("/ServiceHub/v1/users/" + clientId + "/role"), Map.of("role", "PROVIDER")), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROVIDER"));
    }

    @Test
    @DisplayName("RN05 (anti-BOLA): outro prestador não altera nem exclui o serviço (403)")
    void rn05_notTheOwner() throws Exception {
        String owner = tokenFor("PROVIDER");
        String other = tokenFor("PROVIDER");
        long serviceId = createService(owner, "100.00");

        updateService(other, serviceId, "1.00", null, 403);
        mockMvc.perform(authed(delete("/ServiceHub/v1/services/" + serviceId), other)).andExpect(status().isForbidden());
        // ADMIN pode
        updateService(adminToken(), serviceId, "120.00", null, 200);
    }

    @Test
    @DisplayName("RN06: ADMIN não abre ordens (403); ninguém contrata o próprio serviço (409)")
    void rn06_whoCanHire() throws Exception {
        String provider = tokenFor("PROVIDER");
        long serviceId = createService(provider, "100.00");

        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests"), Map.of("serviceId", serviceId)), adminToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests"), Map.of("serviceId", serviceId)), provider))
                .andExpect(status().isConflict());
        // outro PROVIDER pode contratar (um prestador também contrata outros prestadores)
        String otherProvider = tokenFor("PROVIDER");
        createOrder(otherProvider, serviceId);
        // serviço inexistente: 404
        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/service-requests"), Map.of("serviceId", 99999999)), otherProvider))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RN10: serviço e usuário com ordens ativas não podem ser desativados/excluídos (409)")
    void rn10_activeOrdersBlockDeactivation() throws Exception {
        String provider = tokenFor("PROVIDER");
        String client = tokenFor("CLIENT");
        long providerId = meId(provider);
        long serviceId = createService(provider, "100.00");
        long orderId = createOrder(client, serviceId);

        updateService(provider, serviceId, "100.00", false, 409);
        mockMvc.perform(authed(delete("/ServiceHub/v1/services/" + serviceId), provider)).andExpect(status().isConflict());
        String admin = adminToken();
        mockMvc.perform(authed(delete("/ServiceHub/v1/users/" + providerId), admin)).andExpect(status().isConflict());
        mockMvc.perform(authed(jsonBody(patch("/ServiceHub/v1/users/" + providerId + "/active"), Map.of("active", false)), admin))
                .andExpect(status().isConflict());

        // depois de cancelar a ordem, a exclusão lógica passa (204)
        changeStatus(client, orderId, "CANCELLED", 200);
        mockMvc.perform(authed(delete("/ServiceHub/v1/services/" + serviceId), provider)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("RN11 e RN12: desativar usuário desativa serviços, bloqueia login e invalida o token; serviço inativo dá 404 ao público")
    void rn11_rn12_deactivation() throws Exception {
        String providerEmail = uniqueEmail("saindo");
        register(providerEmail, "PROVIDER");
        String provider = login(providerEmail, PASSWORD);
        long providerId = meId(provider);
        long serviceId = createService(provider, "100.00");
        mockMvc.perform(get("/ServiceHub/v1/services/" + serviceId)).andExpect(status().isOk());

        String admin = adminToken();
        mockMvc.perform(authed(delete("/ServiceHub/v1/users/" + providerId), admin)).andExpect(status().isNoContent());

        // RN11: não loga mais e o token que já tinha deixa de valer
        mockMvc.perform(jsonBody(post("/ServiceHub/v1/auth/login"), Map.of("email", providerEmail, "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(authed(get("/ServiceHub/v1/auth/me"), provider)).andExpect(status().isUnauthorized());

        // RN12: serviço inativo -> 404 ao público; ADMIN continua vendo
        mockMvc.perform(get("/ServiceHub/v1/services/" + serviceId)).andExpect(status().isNotFound());
        mockMvc.perform(authed(get("/ServiceHub/v1/services/" + serviceId), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/ServiceHub/v1/services").param("providerId", String.valueOf(providerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("RN13: uma pessoa só vê o próprio perfil e as ordens em que participa (ADMIN vê tudo)")
    void rn13_visibility() throws Exception {
        String provider = tokenFor("PROVIDER");
        String client = tokenFor("CLIENT");
        String stranger = tokenFor("CLIENT");
        long clientId = meId(client);
        long orderId = createOrder(client, createService(provider, "100.00"));

        mockMvc.perform(authed(get("/ServiceHub/v1/users/" + clientId), stranger)).andExpect(status().isForbidden());
        mockMvc.perform(authed(get("/ServiceHub/v1/users/" + clientId), client)).andExpect(status().isOk());
        mockMvc.perform(authed(get("/ServiceHub/v1/users/" + clientId), adminToken())).andExpect(status().isOk());

        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests/" + orderId), stranger)).andExpect(status().isForbidden());
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests/" + orderId), provider)).andExpect(status().isOk());

        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests"), stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests").param("role", "client"), client))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests").param("role", "provider"), client))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mockMvc.perform(authed(get("/ServiceHub/v1/service-requests").param("role", "provider").param("status", "PENDING"), provider))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mockMvc.perform(authed(get("/ServiceHub/v1/users"), client)).andExpect(status().isForbidden());
        mockMvc.perform(authed(get("/ServiceHub/v1/users"), adminToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Contrato: paginação (padrão 20, máximo 100), filtros e busca por título")
    void paginationAndFilters() throws Exception {
        String provider = tokenFor("PROVIDER");
        createService(provider, "10.00");
        createService(provider, "20.00");

        mockMvc.perform(get("/ServiceHub/v1/services").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
        mockMvc.perform(get("/ServiceHub/v1/services"))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0));
        mockMvc.perform(get("/ServiceHub/v1/services").param("providerId", String.valueOf(meId(provider)))
                        .param("minPrice", "15").param("q", "ELÉTRICA"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].price").value(20.00));
        // ordenação por campo inexistente: 400, nunca 500
        mockMvc.perform(get("/ServiceHub/v1/services").param("sort", "campoQueNaoExiste"))
                .andExpect(status().isBadRequest());
        // enum inválido no filtro: 400 listando os valores aceitos
        mockMvc.perform(get("/ServiceHub/v1/services").param("category", "NAO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("ELETRICA")));
    }

    @Test
    @DisplayName("Validação: serviço sem preço válido responde 400 com errors; HTML do texto livre é escapado")
    void validationAndSanitization() throws Exception {
        String provider = tokenFor("PROVIDER");

        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Ab", "price", "-5", "category", "ELETRICA")), provider))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[?(@.field=='price')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='title')]").exists());

        mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Pintura geral", "description", "<script>alert(1)</script>",
                                "price", "80.00", "category", "PINTURA")), provider))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    @DisplayName("Mass assignment: providerId e active enviados no POST de serviço são ignorados")
    void massAssignmentIsIgnored() throws Exception {
        String provider = tokenFor("PROVIDER");
        String victim = tokenFor("PROVIDER");
        MvcResult result = mockMvc.perform(authed(jsonBody(post("/ServiceHub/v1/services"),
                        Map.of("title", "Serviço legítimo", "price", "50.00", "category", "LIMPEZA",
                                "providerId", meId(victim), "active", false, "id", 12345)), provider))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(read(result, "$.providerId")).isEqualTo(meId(provider));
        org.assertj.core.api.Assertions.assertThat(read(result, "$.id")).isNotEqualTo(12345L);
    }
}
