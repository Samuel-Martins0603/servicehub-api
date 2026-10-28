package br.com.servicehub.api.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Converte QUALQUER erro em application/problem+json (RFC 9457) com type, title, status, detail,
 * instance e timestamp. Nunca devolve stack trace; o erro real de 500 fica so no log (Mapa, secao 5).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- erros de negocio ----------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex, HttpServletRequest request) {
        ProblemDetail pd = Problems.of(ex.getStatus(), ex.getTypeSuffix(), ex.getTitle(), ex.getMessage(),
                request.getRequestURI());
        if (!ex.getErrors().isEmpty()) {
            pd.setProperty("errors", toMaps(ex.getErrors()));
        }
        return respond(ex.getStatus(), pd);
    }

    // ---------- seguranca (lancadas por @PreAuthorize dentro do controller) ----------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, Problems.of(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado",
                "Você não tem permissão para acessar este recurso.", request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, Problems.of(HttpStatus.UNAUTHORIZED, "unauthorized",
                "Não autenticado", "Token ausente, inválido ou expirado.", request.getRequestURI()));
    }

    // ---------- integridade do banco (ultima linha de defesa) ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacao de integridade [requestId={}]: {}", MDC.get("requestId"), ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, Problems.of(HttpStatus.CONFLICT, "business-rule",
                "Regra de negócio violada", "A operação viola uma restrição de integridade dos dados.",
                request.getRequestURI()));
    }

    // ---------- 500: detail generico, erro real so no log ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        log.error("Erro inesperado [requestId={}]", requestId, ex);
        String detail = "Erro inesperado. Informe o X-Request-Id" + (requestId != null ? " (" + requestId + ")" : "")
                + " ao suporte.";
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, Problems.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error", "Erro interno", detail, request.getRequestURI()));
    }

    // ---------- erros padrao do Spring MVC, ajustados ao nosso formato ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "inválido" : fe.getDefaultMessage()))
                .toList();
        ProblemDetail pd = Problems.of(HttpStatus.BAD_REQUEST, "validation-error", "Dados inválidos",
                "Um ou mais campos estão inválidos.", path(request));
        pd.setProperty("errors", errors);
        return respond(HttpStatus.BAD_REQUEST, pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = "Corpo da requisição ausente ou JSON malformado.";
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String field = ife.getPath().isEmpty() ? "valor" : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            detail = "Valor inválido para o campo '" + field + "'. Valores aceitos: "
                    + Arrays.toString(ife.getTargetType().getEnumConstants()) + ".";
        }
        return respond(HttpStatus.BAD_REQUEST, Problems.of(HttpStatus.BAD_REQUEST, "validation-error",
                "Dados inválidos", detail, path(request)));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String detail = "Valor inválido para o parâmetro '" + ex.getPropertyName() + "'.";
        Class<?> required = ex.getRequiredType();
        if (required != null && required.isEnum()) {
            detail += " Valores aceitos: " + Arrays.toString(required.getEnumConstants()) + ".";
        }
        return respond(HttpStatus.BAD_REQUEST, Problems.of(HttpStatus.BAD_REQUEST, "validation-error",
                "Dados inválidos", detail, path(request)));
    }

    /** Demais erros do Spring MVC (404 de rota, 405, 415, parametro ausente...): completa type/timestamp. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd) {
            HttpStatus http = HttpStatus.resolve(statusCode.value());
            if (pd.getType() == null || "about:blank".equals(pd.getType().toString())) {
                pd.setType(java.net.URI.create(Problems.TYPE_PREFIX + suffixFor(statusCode.value())));
            }
            if (pd.getTitle() == null && http != null) {
                pd.setTitle(http.getReasonPhrase());
            }
            if (pd.getInstance() == null && path(request) != null) {
                try {
                    pd.setInstance(java.net.URI.create(path(request)));
                } catch (IllegalArgumentException ignored) {
                    // instance e opcional
                }
            }
            pd.setProperty("timestamp", java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        }
        return response;
    }

    // ---------- helpers ----------

    private static ResponseEntity<Object> respond(HttpStatus status, ProblemDetail pd) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private static List<Map<String, String>> toMaps(List<ApiException.FieldError> errors) {
        return errors.stream().map(e -> Map.of("field", e.field(), "message", e.message())).toList();
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : null;
    }

    private static String suffixFor(int status) {
        return switch (status) {
            case 400 -> "validation-error";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not-found";
            case 405 -> "method-not-allowed";
            case 415 -> "unsupported-media-type";
            default -> status >= 500 ? "internal-error" : "bad-request";
        };
    }
}
