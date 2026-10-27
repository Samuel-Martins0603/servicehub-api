package br.com.servicehub.api.model;

/**
 * Categorias de servico: os 21 valores do CHECK chk_services_category do servicehub_schema.sql
 * (Mapa, secao 3). Precisa ser IDENTICA ao CHECK da migracao V3 e ao enum de graphql/schema.graphqls.
 * Nova categoria = nova migracao.
 */
public enum ServiceCategory {
    LIMPEZA, DIARISTA, ENCANAMENTO, ELETRICA, JARDINAGEM, PINTURA,
    MARCENARIA, PEDREIRO, DEDETIZACAO, AR_CONDICIONADO, CHAVEIRO,
    MONTAGEM_MOVEIS, MUDANCA, SUPORTE_TECNICO, MANUTENCAO_COMPUTADORES,
    DESENVOLVIMENTO_SOFTWARE, REDES_E_INFRAESTRUTURA, SEGURANCA_DIGITAL,
    RECUPERACAO_DE_DADOS, CONSULTORIA_TI, OUTROS
}
