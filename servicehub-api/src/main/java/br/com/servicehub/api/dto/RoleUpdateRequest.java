package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull(message = "é obrigatório")
        Role role
) {
}
