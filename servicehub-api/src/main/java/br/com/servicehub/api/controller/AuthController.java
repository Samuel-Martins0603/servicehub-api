package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.LoginRequest;
import br.com.servicehub.api.dto.RegisterRequest;
import br.com.servicehub.api.dto.TokenResponse;
import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.security.AuthUser;
import br.com.servicehub.api.service.AuthService;
import br.com.servicehub.api.service.UserService;
import br.com.servicehub.api.web.ProblemResponses;
import br.com.servicehub.api.web.PublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Cadastro, login (JWT) e usuário autenticado")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PublicEndpoint
    @PostMapping("/register")
    @Operation(summary = "Cadastra um usuário (CLIENT ou PROVIDER)",
            description = "201 com o usuário e header Location. O papel ADMIN no cadastro responde 400 (RN03).")
    @ProblemResponses({400, 409, 500})
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @PublicEndpoint
    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve um token JWT (HS256, 24h)")
    @ProblemResponses({400, 401, 500})
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve o usuário autenticado")
    @ProblemResponses({401, 500})
    public UserResponse me(@AuthenticationPrincipal AuthUser actor) {
        return userService.me(actor);
    }
}
