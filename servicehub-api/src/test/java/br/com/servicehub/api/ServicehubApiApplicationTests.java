package br.com.servicehub.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de contexto: sobe tudo (Flyway aplica V1..V3 e o Hibernate valida as entidades contra o schema
 * com ddl-auto=validate). Se faltar uma migracao para uma coluna da entidade, este teste falha.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicehubApiApplicationTests {

    @Test
    void contextLoads() {
        // o proprio carregamento do contexto e o teste
    }
}
