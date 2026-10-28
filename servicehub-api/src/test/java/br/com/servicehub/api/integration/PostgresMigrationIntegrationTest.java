package br.com.servicehub.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integracao com PostgreSQL REAL (Testcontainers): as migracoes Flyway aplicam do zero e o Hibernate
 * valida as entidades (ddl-auto=validate) contra o schema do Mapa. Sem Docker, o teste e ignorado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class PostgresMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.placeholders.textType", () -> "TEXT");
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("PostgreSQL real: migrações V1..V3 aplicam e o schema final tem as 4 tabelas do Mapa")
    void migrationsApplyOnRealPostgres() {
        Integer applied = jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class);
        assertThat(applied).isEqualTo(3);
        for (String table : new String[]{"users", "services", "service_requests", "reviews"}) {
            Integer exists = jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_name = ?", Integer.class, table);
            assertThat(exists).as("tabela " + table).isEqualTo(1);
        }
        Integer legacy = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'servicos'", Integer.class);
        assertThat(legacy).isZero();
    }

    @Test
    @DisplayName("Banco: CHECK recusa papel inválido e e-mail com maiúsculas (segunda linha de defesa)")
    void checkConstraintsRejectInvalidData() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into users (full_name, email, password_hash, role) values ('X Y','a@b.com','h','SUPERUSER')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into users (full_name, email, password_hash, role) values ('X Y','Maiuscula@B.com','h','CLIENT')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
