package br.com.servicehub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 *
 * @SpringBootApplication é uma "anotação-atalho" que combina três outras:
 *  - @Configuration: esta classe pode declarar beans;
 *  - @EnableAutoConfiguration: o Spring configura sozinho o que encontrar
 *    no classpath (ex.: como há um driver H2 e o starter JPA, ele já monta
 *    o DataSource e o EntityManager para nós);
 *  - @ComponentScan: o Spring varre o pacote br.com.servicehub.api (e
 *    subpacotes) procurando por @RestController, @Service, @Repository etc.
 */
@SpringBootApplication
public class ServicehubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServicehubApiApplication.class, args);
    }
}
