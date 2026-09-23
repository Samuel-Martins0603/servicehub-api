package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) usado como corpo (@RequestBody) do POST e do PUT.
 *
 * Por que não usar a entidade Servico direto no controller? Duas razões:
 *  1) O cliente não deve poder enviar o campo "id" na criação — quem gera
 *     o id é o banco (@GeneratedValue).
 *  2) Aqui é o lugar certo para colocar Bean Validation (@NotBlank,
 *     @Positive...) e a documentação do Swagger (@Schema), sem "sujar"
 *     a entidade JPA com essas preocupações.
 *
 * O record do Java já gera construtor, getters (nome do campo, sem "get"),
 * equals/hashCode e toString — perfeito para um DTO imutável.
 */
@Schema(description = "Dados de entrada para criar ou atualizar um serviço")
public record ServicoRequestDTO(

        @Schema(description = "Nome do serviço", example = "Corte de cabelo")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(description = "Descrição detalhada do serviço", example = "Corte tradicional com máquina e tesoura")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "Preço do serviço em reais", example = "45.90")
        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal preco,

        @Schema(description = "Duração estimada em minutos", example = "30")
        @NotNull(message = "A duração é obrigatória")
        @Positive(message = "A duração deve ser maior que zero")
        Integer duracaoMinutos,

        @Schema(description = "Indica se o serviço está ativo para agendamento", example = "true")
        Boolean ativo
) {
}
