package br.com.servicehub.api.exception;

/**
 * Exceção de negócio: lançada pelo Service quando um recurso pedido
 * (por exemplo, um Servico com um determinado id) não existe.
 *
 * Ela é uma RuntimeException "sem checked exception" de propósito, para
 * não obrigar todo mundo que chama o Service a colocar try/catch — quem
 * trata isso de forma centralizada é o GlobalExceptionHandler, convertendo
 * para um 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
