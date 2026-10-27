package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

@Schema(description = "Credenciais de login")
public record LoginRequest(

        @Schema(example = "maria.silva@email.com")
        @NotBlank(message = "é obrigatório")
        String email,

        @Schema(example = "Senha123")
        @NotBlank(message = "é obrigatória")
        String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
