package br.com.servicehub.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * @RestControllerAdvice intercepta exceções lançadas por QUALQUER controller
 * da aplicação e as converte em respostas HTTP consistentes. É a peça que
 * garante "nenhuma lógica de negócio (nem de tratamento de erro) no
 * Controller" — o controller só deixa a exceção subir, e é aqui que ela
 * vira um 404, 400 etc.
 *
 * Duas exceções tratadas:
 *  1) ResourceNotFoundException -> 404 Not Found
 *  2) MethodArgumentNotValidException -> 400 Bad Request
 *     (disparada automaticamente pelo Spring quando um @Valid falha,
 *     por exemplo um "preco" negativo ou um "nome" em branco)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados inválidos no corpo da requisição",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
