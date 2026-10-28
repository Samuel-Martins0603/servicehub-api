package br.com.servicehub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/** Ponto de entrada. Datas da API sao em horario de Brasilia (Mapa, secao 5). */
@SpringBootApplication
public class ServicehubApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(ServicehubApiApplication.class, args);
    }
}
