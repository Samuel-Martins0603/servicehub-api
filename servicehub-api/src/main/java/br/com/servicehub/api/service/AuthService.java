package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.LoginRequest;
import br.com.servicehub.api.dto.RegisterRequest;
import br.com.servicehub.api.dto.TokenResponse;
import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Cadastro e login. Regras: RN01 (e-mail unico), RN02 (BCrypt), RN03 (papel no cadastro), RN11 (conta inativa). */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    // Hash descartavel: o login gasta o mesmo tempo de BCrypt mesmo quando o e-mail nao existe.
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.dummyHash = encoder.encode("senha-descartavel-Aa1");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // RN03: no cadastro o papel so pode ser CLIENT ou PROVIDER.
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("role", "deve ser CLIENT ou PROVIDER");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        // RN01: e-mail unico, sem diferenciar maiusculas.
        if (users.existsByEmail(email)) {
            throw ApiException.emailAlreadyExists();
        }
        // RN02: so o hash BCrypt e gravado.
        User user = new User(request.fullName().trim(), email, encoder.encode(request.password()), request.role());
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Duas requisicoes simultaneas: a constraint UNIQUE do banco garante a RN01.
            throw ApiException.emailAlreadyExists();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElse(null);
        boolean passwordOk = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        // Mesma resposta para e-mail inexistente, senha errada e conta inativa (RN11).
        if (user == null || !passwordOk || !user.isActive()) {
            throw ApiException.unauthorized("Credenciais inválidas ou conta inativa.");
        }
        return new TokenResponse(jwtService.generate(user), "Bearer", jwtService.getExpirationSeconds());
    }
}
