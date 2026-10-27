package br.com.servicehub.api.security;

import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le "Authorization: Bearer <token>", valida o JWT e CONFERE O USUARIO NO BANCO a cada requisicao:
 * se estiver inativo (ou nao existir mais), nao autentica e a rota protegida responde 401 (RN11).
 * O papel usado e o do banco, nao o do token, entao mudanca de papel vale imediatamente.
 * Nao e um @Component de proposito: e criado dentro da SecurityFilterChain (evita registro duplicado).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parse(token);
                Object uid = claims.get("uid");
                if (uid instanceof Number number) {
                    userRepository.findById(number.longValue())
                            .filter(User::isActive)
                            .ifPresent(this::authenticate);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                // Token invalido/expirado: segue anonimo; rotas protegidas respondem 401 no entry point.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(User user) {
        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
