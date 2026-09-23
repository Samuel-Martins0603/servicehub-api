package br.com.servicehub.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Camada de configuração (não é Controller, nem Service, nem Repository —
 * é infraestrutura da aplicação). Aqui só personalizamos o cabeçalho que
 * aparece no topo da página do Swagger UI: título, descrição e versão.
 *
 * O SpringDoc já gera toda a especificação OpenAPI sozinho a partir dos
 * @RestController; este bean só enriquece as informações gerais da API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI servicehubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description(
                                "API REST para gestão de serviços do ServiceHub. "
                                        + "Projeto de Checkpoint da disciplina de Engenharia de Software.")
                        .version("v0.1 (checkpoint)")
                        .contact(new Contact()
                                .name("Equipe ServiceHub")
                                .email("equipe@servicehub.example.com")));
    }
}
