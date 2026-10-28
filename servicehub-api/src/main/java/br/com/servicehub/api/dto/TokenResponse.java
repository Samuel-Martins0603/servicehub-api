package br.com.servicehub.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT emitido no login")
public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "86400", description = "Validade em segundos") long expiresIn
) {
}
