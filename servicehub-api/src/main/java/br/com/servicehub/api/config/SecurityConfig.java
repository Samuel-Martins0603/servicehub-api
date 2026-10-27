package br.com.servicehub.api.config;

import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.security.JwtAuthenticationFilter;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.security.RestAccessDeniedHandler;
import br.com.servicehub.api.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguranca (Mapa, secao 10): SecurityFilterChain, sessao STATELESS, CSRF desabilitado (a API usa
 * Bearer token, nao cookie), JWT, @PreAuthorize por papel e CORS com origens explicitas.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** RN02: senhas somente como hash BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UserRepository userRepository,
                                           RestAuthenticationEntryPoint entryPoint,
                                           RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // rotas publicas (Mapa, secao 10)
                        .requestMatchers(HttpMethod.POST, "/ServiceHub/v1/auth/register", "/ServiceHub/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/ServiceHub/v1/services",
                                "/ServiceHub/v1/services/{id}",
                                "/ServiceHub/v1/services/{id}/reviews",
                                "/ServiceHub/v1/reviews/{id}",
                                "/ServiceHub/v1/info").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/docs", "/docs/**", "/api-docs", "/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // endpoint alternativo GraphQL: so consultas ao catalogo publico
                        .requestMatchers(HttpMethod.POST, "/graphql").permitAll()
                        .requestMatchers("/error").permitAll()
                        // metrics e prometheus: somente ADMIN
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // todo o resto exige token
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS: origens vindas de CORS_ALLOWED_ORIGINS (incluindo http://localhost:5173), nunca "*". */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        config.setExposedHeaders(List.of("Location", "X-Request-Id"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
