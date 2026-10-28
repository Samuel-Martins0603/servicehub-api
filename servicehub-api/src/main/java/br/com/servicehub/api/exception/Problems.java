package br.com.servicehub.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Fabrica de ProblemDetail (RFC 9457) no formato do Mapa, secao 5. */
public final class Problems {

    public static final String TYPE_PREFIX = "urn:servicehub:problem:";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatus status, String typeSuffix, String title, String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_PREFIX + typeSuffix));
        pd.setTitle(title);
        if (instance != null) {
            try {
                pd.setInstance(URI.create(instance));
            } catch (IllegalArgumentException ignored) {
                // instance e opcional; ignora URI malformada
            }
        }
        pd.setProperty("timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        return pd;
    }
}
