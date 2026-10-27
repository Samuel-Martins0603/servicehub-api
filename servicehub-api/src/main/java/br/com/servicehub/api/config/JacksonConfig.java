package br.com.servicehub.api.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/** Datas em ISO-8601 sem fuso e sem fracao de segundos: 2026-11-05T14:30:00 (Mapa, secao 5). */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeFormat() {
        return builder -> builder.serializers(new LocalDateTimeSerializer(ISO_SECONDS));
    }
}
