package br.com.servicehub.api.config;

import br.com.servicehub.api.web.ProblemResponses;
import br.com.servicehub.api.web.PublicEndpoint;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * OpenAPI/Swagger em /docs: esquema Bearer JWT (botao Authorize), schema ProblemDetail e,
 * para cada endpoint, os status codes de erro declarados em @ProblemResponses.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    private static final Map<Integer, String> DESCRIPTIONS = Map.of(
            400, "Dados inválidos (validation-error)",
            401, "Não autenticado (unauthorized)",
            403, "Sem permissão (forbidden)",
            404, "Recurso não encontrado (not-found)",
            409, "Regra de negócio violada (email-already-exists, business-rule ou invalid-transition)",
            500, "Erro inesperado (internal-error); informe o X-Request-Id");

    @Bean
    public OpenAPI servicehubOpenAPI(@Value("${app.version:1.0.0}") String version) {
        Schema<?> problem = new ObjectSchema()
                .description("ProblemDetail (RFC 9457), application/problem+json")
                .addProperty("type", new StringSchema().example("urn:servicehub:problem:validation-error"))
                .addProperty("title", new StringSchema().example("Dados inválidos"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Um ou mais campos estão inválidos."))
                .addProperty("instance", new StringSchema().example("/api/v1/services"))
                .addProperty("timestamp", new StringSchema().example("2026-11-05T17:30:12Z"))
                .addProperty("errors", new ArraySchema().items(new ObjectSchema()
                        .addProperty("field", new StringSchema().example("price"))
                        .addProperty("message", new StringSchema().example("deve ser maior que 0"))));

        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("API REST do ServiceHub: prestadores publicam servicos, clientes contratam "
                                + "(ordens de servico) e avaliam. Ciclo 3: refatoracao conforme o Mapa de Construcao.")
                        .version(version)
                        .contact(new Contact().name("Equipe ServiceHub")))
                .components(new Components()
                        .addSchemas("ProblemDetail", problem)
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer problemResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getMethodAnnotation(PublicEndpoint.class) == null) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER));
            }
            ProblemResponses declared = handlerMethod.getMethodAnnotation(ProblemResponses.class);
            if (declared != null) {
                if (operation.getResponses() == null) {
                    operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
                }
                for (int code : declared.value()) {
                    Schema<?> ref = new Schema<>().$ref("#/components/schemas/ProblemDetail");
                    ApiResponse response = new ApiResponse()
                            .description(DESCRIPTIONS.getOrDefault(code, "Erro"))
                            .content(new Content().addMediaType("application/problem+json", new MediaType().schema(ref)));
                    operation.getResponses().addApiResponse(String.valueOf(code), response);
                }
            }
            return operation;
        };
    }
}
