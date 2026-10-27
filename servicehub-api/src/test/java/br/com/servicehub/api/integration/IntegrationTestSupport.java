package br.com.servicehub.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integracao: sobe a aplicacao inteira (H2 em memoria, Flyway, Security, JWT) e
 * oferece helpers para cadastrar/logar usuarios e chamar a API como um cliente real.
 * Todos os e-mails sao unicos por teste (o contexto e compartilhado entre as classes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger();
    static final String ADMIN_EMAIL = "admin@servicehub.test";
    static final String ADMIN_PASSWORD = "Admin1234";
    static final String PASSWORD = "Senha123";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    protected String uniqueEmail(String prefix) {
        return prefix + SEQ.incrementAndGet() + "." + System.nanoTime() + "@teste.com";
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected long read(MvcResult result, String path) throws Exception {
        Number n = JsonPath.read(result.getResponse().getContentAsString(), path);
        return n.longValue();
    }

    protected String readString(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /** Cadastra e devolve o id do usuario. */
    protected long register(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(jsonBody(post("/api/v1/auth/register"),
                        Map.of("fullName", "Usuario Teste", "email", email, "password", PASSWORD, "role", role)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, "$.id");
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(jsonBody(post("/api/v1/auth/login"),
                        Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn();
        return readString(result, "$.accessToken");
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    /** Cadastra um usuario do papel dado e devolve o token dele. */
    protected String tokenFor(String role) throws Exception {
        String email = uniqueEmail(role.toLowerCase());
        register(email, role);
        return login(email, PASSWORD);
    }

    protected long meId(String token) throws Exception {
        MvcResult result = mockMvc.perform(authed(get("/api/v1/auth/me"), token)).andExpect(status().isOk()).andReturn();
        return read(result, "$.id");
    }

    protected long createService(String providerToken, String price) throws Exception {
        MvcResult result = mockMvc.perform(authed(jsonBody(post("/api/v1/services"),
                        Map.of("title", "Instalação elétrica residencial", "description", "Tomadas, disjuntores e fiação",
                                "price", price, "category", "ELETRICA")), providerToken))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, "$.id");
    }

    protected long createOrder(String clientToken, long serviceId) throws Exception {
        MvcResult result = mockMvc.perform(authed(jsonBody(post("/api/v1/service-requests"),
                        Map.of("serviceId", serviceId, "notes", "Apartamento 302")), clientToken))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, "$.id");
    }

    protected void changeStatus(String token, long orderId, String status, int expectedHttp) throws Exception {
        mockMvc.perform(authed(jsonBody(patch("/api/v1/service-requests/" + orderId + "/status"),
                        Map.of("status", status)), token))
                .andExpect(status().is(expectedHttp));
    }

    protected void updateService(String token, long serviceId, String price, Boolean active, int expectedHttp) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Instalação elétrica residencial");
        body.put("description", "Tomadas, disjuntores e fiação");
        body.put("price", price);
        body.put("category", "ELETRICA");
        if (active != null) {
            body.put("active", active);
        }
        mockMvc.perform(authed(jsonBody(put("/api/v1/services/" + serviceId), body), token))
                .andExpect(status().is(expectedHttp));
    }
}
