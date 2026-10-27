package br.com.servicehub.api.security;

import br.com.servicehub.api.model.Role;

/** Usuario autenticado (principal do Spring Security). Montado a partir do BANCO a cada requisicao. */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
