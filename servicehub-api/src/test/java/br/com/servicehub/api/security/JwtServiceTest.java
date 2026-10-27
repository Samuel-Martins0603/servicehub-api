package br.com.servicehub.api.security;

import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.support.TestData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    @DisplayName("Segurança: segredo com menos de 32 bytes impede a aplicação de subir")
    void secretShorterThan32BytesIsRejected() {
        assertThatThrownBy(() -> new JwtService("curto", 86400))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("Segurança: token HS256 carrega sub, uid, roles, iat e exp (24h)")
    void tokenHasExpectedClaims() {
        JwtService jwt = new JwtService(SECRET, 86400);
        User user = TestData.user(7L, "Maria Silva", Role.PROVIDER);

        Claims claims = jwt.parse(jwt.generate(user));

        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(((Number) claims.get("uid")).longValue()).isEqualTo(7L);
        assertThat(claims.get("roles", List.class)).containsExactly("PROVIDER");
        assertThat(claims.getIssuedAt()).isNotNull();
        long lifetimeSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertThat(lifetimeSeconds).isEqualTo(86400L);
    }

    @Test
    @DisplayName("Segurança: token adulterado ou assinado com outro segredo é rejeitado")
    void tamperedOrForeignTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 86400);
        JwtService other = new JwtService("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ", 86400);
        User user = TestData.user(1L, "Joao Pereira", Role.CLIENT);

        assertThatThrownBy(() -> jwt.parse(other.generate(user))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.parse(jwt.generate(user) + "x")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Segurança: token expirado é rejeitado")
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -10);
        User user = TestData.user(1L, "Joao Pereira", Role.CLIENT);

        assertThatThrownBy(() -> jwt.parse(jwt.generate(user))).isInstanceOf(JwtException.class);
    }
}
