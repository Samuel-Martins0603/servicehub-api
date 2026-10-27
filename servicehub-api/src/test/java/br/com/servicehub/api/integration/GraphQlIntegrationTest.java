package br.com.servicehub.api.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoint alternativo GraphQL (Aula 9): consulta publica do catalogo, com as mesmas regras do REST. */
class GraphQlIntegrationTest extends IntegrationTestSupport {

    private String graphql(String query) throws Exception {
        MvcResult started = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("query", query))))
                .andReturn();
        // o handler MVC do Spring GraphQL pode responder de forma assincrona
        MvcResult done = started.getRequest().isAsyncStarted()
                ? mockMvc.perform(asyncDispatch(started)).andReturn()
                : started;
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        return done.getResponse().getContentAsString();
    }

    @Test
    @DisplayName("GraphQL: consulta pública do catálogo funciona sem token")
    void publicCatalogQuery() throws Exception {
        String provider = tokenFor("PROVIDER");
        long serviceId = createService(provider, "150.00");

        String body = graphql("{ service(id: " + serviceId + ") { id title price category providerName active } }");

        assertThat(JsonPath.<String>read(body, "$.data.service.title")).isEqualTo("Instalação elétrica residencial");
        assertThat(JsonPath.<String>read(body, "$.data.service.category")).isEqualTo("ELETRICA");
        assertThat(JsonPath.<Boolean>read(body, "$.data.service.active")).isTrue();
        assertThat(JsonPath.<Object>read(body, "$.errors")).isNull();
    }

    @Test
    @DisplayName("GraphQL: lista paginada com filtros e limite de 100 itens por página")
    void pagedQueryWithFilters() throws Exception {
        String provider = tokenFor("PROVIDER");
        long providerId = meId(provider);
        createService(provider, "10.00");
        createService(provider, "30.00");

        String body = graphql("{ services(providerId: " + providerId + ", minPrice: 20, size: 500) "
                + "{ size totalElements content { price } } }");

        assertThat(JsonPath.<Integer>read(body, "$.data.services.size")).isEqualTo(100);
        assertThat(JsonPath.<Integer>read(body, "$.data.services.totalElements")).isEqualTo(1);
        List<Object> prices = JsonPath.read(body, "$.data.services.content[*].price");
        assertThat(prices).hasSize(1);
    }

    @Test
    @DisplayName("GraphQL + RN12: serviço excluído (inativo) retorna null para o público")
    void inactiveServiceIsNullForThePublic() throws Exception {
        String provider = tokenFor("PROVIDER");
        long serviceId = createService(provider, "150.00");
        mockMvc.perform(authed(delete("/ServiceHub/v1/services/" + serviceId), provider)).andExpect(status().isNoContent());

        String body = graphql("{ service(id: " + serviceId + ") { id title } }");

        assertThat(JsonPath.<Object>read(body, "$.data.service")).isNull();
    }
}
