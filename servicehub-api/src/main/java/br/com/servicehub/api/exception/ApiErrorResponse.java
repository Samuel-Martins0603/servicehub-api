package br.com.servicehub.api.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de erro devolvido pela API, para o cliente (e o Swagger)
 * saber sempre o que esperar quando algo dá errado — em vez de cada
 * endpoint devolver um JSON de erro diferente.
 */
@Schema(description = "Estrutura padrão de erro da API")
public record ApiErrorResponse(

        @Schema(description = "Momento em que o erro ocorreu")
        Instant timestamp,

        @Schema(description = "Código de status HTTP", example = "404")
        int status,

        @Schema(description = "Categoria do erro", example = "Not Found")
        String error,

        @Schema(description = "Mensagem descritiva do erro", example = "Serviço com id 42 não encontrado")
        String message,

        @Schema(description = "Lista de erros de validação de campo, quando aplicável")
        List<String> details
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(Instant.now(), status, error, message, List.of());
    }

    public static ApiErrorResponse of(int status, String error, String message, List<String> details) {
        return new ApiErrorResponse(Instant.now(), status, error, message, details);
    }
}
