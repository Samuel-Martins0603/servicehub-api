package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo status da ordem")
public record StatusUpdateRequest(
        @Schema(example = "ACCEPTED")
        @NotNull(message = "é obrigatório")
        RequestStatus status
) {
}
