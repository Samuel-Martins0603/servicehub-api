package br.com.servicehub.api.service;

import org.springframework.web.util.HtmlUtils;

/**
 * Sanitizacao (Aula 8): campos de texto livre (description, notes, comment, bio) passam por escape
 * de HTML na camada de servico ANTES de gravar. Vazio vira null.
 */
public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : HtmlUtils.htmlEscape(trimmed);
    }
}
