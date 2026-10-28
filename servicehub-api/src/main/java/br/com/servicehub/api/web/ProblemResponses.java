package br.com.servicehub.api.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lista os status de erro que o endpoint pode devolver (Mapa, secao 6).
 * O OpenApiConfig transforma isso em respostas documentadas com o schema ProblemDetail.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProblemResponses {
    int[] value();
}
