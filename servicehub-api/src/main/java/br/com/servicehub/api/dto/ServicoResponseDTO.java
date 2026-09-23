package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO de saída: o que a API devolve ao cliente. Inclui o "id", que faz
 * sentido na resposta (o cliente precisa dele para os próximos GET/PUT/DELETE)
 * mas não fazia sentido no request de criação.
 */
@Schema(description = "Representação de um serviço retornada pela API")
public record ServicoResponseDTO(

        @Schema(description = "Identificador único do serviço", example = "1")
        Long id,

        @Schema(description = "Nome do serviço", example = "Corte de cabelo")
        String nome,

        @Schema(description = "Descrição detalhada do serviço", example = "Corte tradicional com máquina e tesoura")
        String descricao,

        @Schema(description = "Preço do serviço em reais", example = "45.90")
        BigDecimal preco,

        @Schema(description = "Duração estimada em minutos", example = "30")
        Integer duracaoMinutos,

        @Schema(description = "Indica se o serviço está ativo para agendamento", example = "true")
        Boolean ativo
) {
    /** Método de fábrica: converte a entidade JPA no DTO exposto pela API. */
    public static ServicoResponseDTO fromEntity(br.com.servicehub.api.model.Servico servico) {
        return new ServicoResponseDTO(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getDuracaoMinutos(),
                servico.getAtivo()
        );
    }
}
