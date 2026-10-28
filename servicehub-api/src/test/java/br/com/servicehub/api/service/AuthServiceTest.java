package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.LoginRequest;
import br.com.servicehub.api.dto.RegisterRequest;
import br.com.servicehub.api.dto.TokenResponse;
import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.security.JwtService;
import br.com.servicehub.api.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("RN01: e-mail já cadastrado responde 409 email-already-exists")
    void rn01_duplicateEmailIsRejected() {
        when(users.existsByEmail("maria@email.com")).thenReturn(true);

        TestData.assertApiError(
                () -> authService.register(new RegisterRequest("Maria Silva", "maria@email.com", "Senha123", Role.CLIENT)),
                HttpStatus.CONFLICT, "email-already-exists");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN01: e-mail é normalizado (trim + minúsculas) antes de comparar e gravar")
    void rn01_emailIsNormalized() {
        RegisterRequest request = new RegisterRequest("Maria Silva", "  Maria.Silva@EMAIL.com ", "Senha123", Role.CLIENT);
        assertThat(request.email()).isEqualTo("maria.silva@email.com");

        when(users.existsByEmail("maria.silva@email.com")).thenReturn(false);
        when(encoder.encode("Senha123")).thenReturn("$2a$10$hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("maria.silva@email.com");
    }

    @Test
    @DisplayName("RN02: a senha é gravada só como hash BCrypt e nunca aparece na resposta")
    void rn02_passwordIsStoredHashedAndNeverReturned() {
        when(users.existsByEmail("maria@email.com")).thenReturn(false);
        when(encoder.encode("Senha123")).thenReturn("$2a$10$hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("Maria Silva", "maria@email.com", "Senha123", Role.PROVIDER));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$10$hash").isNotEqualTo("Senha123");
        assertThat(Arrays.stream(UserResponse.class.getRecordComponents()).map(c -> c.getName().toLowerCase()))
                .noneMatch(name -> name.contains("password"));
        assertThat(response.role()).isEqualTo(Role.PROVIDER);
    }

    @Test
    @DisplayName("RN03: cadastro com papel ADMIN responde 400")
    void rn03_registerAsAdminIsRejected() {
        TestData.assertApiError(
                () -> authService.register(new RegisterRequest("Eve", "eve@email.com", "Senha123", Role.ADMIN)),
                HttpStatus.BAD_REQUEST, "validation-error");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Login: credenciais corretas devolvem token Bearer")
    void loginReturnsToken() {
        User user = TestData.user(7L, "Maria Silva", Role.CLIENT);
        when(users.findByEmail("maria.silva@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Senha123", "hash")).thenReturn(true);
        when(jwtService.generate(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        TokenResponse token = authService.login(new LoginRequest("maria.silva@test.com", "Senha123"));

        assertThat(token.accessToken()).isEqualTo("jwt-token");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("Login: senha errada responde 401")
    void loginWithWrongPasswordIs401() {
        User user = TestData.user(7L, "Maria Silva", Role.CLIENT);
        when(users.findByEmail("maria.silva@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("errada", "hash")).thenReturn(false);

        TestData.assertApiError(() -> authService.login(new LoginRequest("maria.silva@test.com", "errada")),
                HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    @Test
    @DisplayName("Login: e-mail inexistente responde 401 (mesma resposta da senha errada)")
    void loginWithUnknownEmailIs401() {
        when(users.findByEmail("ninguem@test.com")).thenReturn(Optional.empty());

        TestData.assertApiError(() -> authService.login(new LoginRequest("ninguem@test.com", "Senha123")),
                HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    @Test
    @DisplayName("RN11: usuário inativo não faz login (401)")
    void rn11_inactiveUserCannotLogin() {
        User user = TestData.user(7L, "Maria Silva", Role.CLIENT);
        user.setActive(false);
        when(users.findByEmail("maria.silva@test.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Senha123", "hash")).thenReturn(true);

        TestData.assertApiError(() -> authService.login(new LoginRequest("maria.silva@test.com", "Senha123")),
                HttpStatus.UNAUTHORIZED, "unauthorized");
    }
}
