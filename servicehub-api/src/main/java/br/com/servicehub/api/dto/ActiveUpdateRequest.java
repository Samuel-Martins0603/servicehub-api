package br.com.servicehub.api.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdateRequest(
        @NotNull(message = "é obrigatório")
        Boolean active
) {
}
