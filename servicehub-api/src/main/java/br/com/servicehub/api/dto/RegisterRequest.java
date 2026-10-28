package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Cadastro. Nao existem id, ativo nem outros campos internos aqui (anti mass assignment, OWASP API3).
 * O e-mail e normalizado (trim + minusculas) no proprio construtor, antes da validacao e de gravar.
 */
@Schema(description = "Dados de cadastro de usuario")
public record RegisterRequest(

        @Schema(example = "Maria Silva")
        @NotBlank(message = "é obrigatório")
        @Size(min = 2, max = 100, message = "deve ter entre 2 e 100 caracteres")
        String fullName,

        @Schema(example = "maria.silva@email.com")
        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String email,

        @Schema(example = "Senha123", description = "8 a 72 caracteres, ao menos uma letra maiúscula e um número")
        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "deve conter ao menos uma letra maiúscula e um número")
        String password,

        @Schema(example = "PROVIDER", allowableValues = {"CLIENT", "PROVIDER"})
        @NotNull(message = "é obrigatório")
        Role role
) {
    public RegisterRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
