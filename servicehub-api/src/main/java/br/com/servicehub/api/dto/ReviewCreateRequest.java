package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Avaliacao de uma ordem concluida")
public record ReviewCreateRequest(

        @Schema(example = "5")
        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser no mínimo 1")
        @Max(value = 5, message = "deve ser no máximo 5")
        Integer rating,

        @Schema(example = "Pontual e caprichosa.")
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String comment
) {
}
