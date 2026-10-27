package br.com.servicehub.api.model;

/**
 * Categorias de servico (21 valores). A lista precisa ser IDENTICA a do CHECK da migracao V3
 * e a do enum ServiceCategory em graphql/schema.graphqls.
 * ATENCAO: confira com o arquivo servicehub_schema.sql do professor (secao 3 do Mapa).
 */
public enum ServiceCategory {
    ELETRICA, HIDRAULICA, LIMPEZA, PINTURA, JARDINAGEM, MARCENARIA, ALVENARIA,
    SUPORTE_TI, AULAS_PARTICULARES, BELEZA, SAUDE_BEM_ESTAR, TRANSPORTE, MUDANCA,
    CLIMATIZACAO, CHAVEIRO, DEDETIZACAO, CUIDADOR, PET, EVENTOS, FOTOGRAFIA, OUTROS
}
