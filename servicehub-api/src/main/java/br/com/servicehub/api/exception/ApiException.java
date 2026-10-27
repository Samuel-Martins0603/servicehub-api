package br.com.servicehub.api.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Excecao de negocio da API. Carrega o status HTTP, o sufixo do "type" do ProblemDetail
 * (urn:servicehub:problem:{typeSuffix}) e, opcionalmente, a lista de erros por campo.
 * O GlobalExceptionHandler converte tudo em application/problem+json.
 */
public class ApiException extends RuntimeException {

    public record FieldError(String field, String message) {
    }

    private final HttpStatus status;
    private final String typeSuffix;
    private final String title;
    private final List<FieldError> errors;

    public ApiException(HttpStatus status, String typeSuffix, String title, String detail, List<FieldError> errors) {
        super(detail);
        this.status = status;
        this.typeSuffix = typeSuffix;
        this.title = title;
        this.errors = errors == null ? List.of() : errors;
    }

    public HttpStatus getStatus() { return status; }
    public String getTypeSuffix() { return typeSuffix; }
    public String getTitle() { return title; }
    public List<FieldError> getErrors() { return errors; }

    public static ApiException badRequest(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation-error", "Dados inválidos",
                "Um ou mais campos estão inválidos.", List.of(new FieldError(field, message)));
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Não autenticado", detail, null);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado", detail, null);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, "not-found", "Recurso não encontrado", detail, null);
    }

    public static ApiException emailAlreadyExists() {
        return new ApiException(HttpStatus.CONFLICT, "email-already-exists", "E-mail já cadastrado",
                "Já existe um usuário com este e-mail.", null);
    }

    public static ApiException businessRule(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "business-rule", "Regra de negócio violada", detail, null);
    }

    public static ApiException invalidTransition(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "invalid-transition", "Transição de status inválida", detail, null);
    }
}
