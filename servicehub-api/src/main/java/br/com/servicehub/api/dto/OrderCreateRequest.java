package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** POST /service-requests. Sem clientId, status ou totalPrice: tudo isso e decidido pelo servidor. */
@Schema(description = "Dados para contratar um servico")
public record OrderCreateRequest(

        @Schema(example = "12")
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser positivo")
        Long serviceId,

        @Schema(example = "2026-11-10T09:00:00")
        @Future(message = "deve ser uma data futura")
        LocalDateTime scheduledAt,

        @Schema(example = "Apartamento 302, interfone 3021")
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String notes
) {
}
