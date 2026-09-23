package br.com.servicehub.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de contexto: se o Spring conseguir subir todos os beans (Controller,
 * Service, Repository, DataSource H2, configuração do OpenAPI) sem lançar
 * exceção, este teste passa. É o primeiro "alarme" que pega erro de
 * configuração antes mesmo de testar regra de negócio.
 */
@SpringBootTest
class ServicehubApiApplicationTests {

    @Test
    void contextLoads() {
        // Corpo vazio de propósito: o próprio carregamento do contexto é o teste.
    }
}
