package br.com.servicehub.api.service;

import br.com.servicehub.api.exception.ApiException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Valida os campos de ordenacao aceitos (evita 500 com ?sort=campoQueNaoExiste). */
public final class Pages {

    private Pages() {
    }

    public static Pageable check(Pageable pageable, String... allowed) {
        Set<String> ok = Set.of(allowed);
        for (Sort.Order order : pageable.getSort()) {
            if (!ok.contains(order.getProperty())) {
                throw ApiException.badRequest("sort", "campo de ordenação inválido: " + order.getProperty()
                        + ". Aceitos: " + String.join(", ", allowed));
            }
        }
        return pageable;
    }
}
