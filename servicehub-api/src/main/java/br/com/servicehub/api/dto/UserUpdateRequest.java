package br.com.servicehub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /users/{id}: altera somente nome, telefone, bio e avatar (nunca papel, e-mail ou senha). */
public record UserUpdateRequest(
        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 100, message = "deve ter entre 2 e 100 caracteres")
        String fullName,

        @Size(max = 20, message = "deve ter no máximo 20 caracteres")
        String phone,

        @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
        String bio,

        @Size(max = 500, message = "deve ter no máximo 500 caracteres")
        String avatarUrl
) {
}
