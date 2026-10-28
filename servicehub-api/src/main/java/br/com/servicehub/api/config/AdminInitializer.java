package br.com.servicehub.api.config;

import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cria o primeiro ADMIN no startup a partir de variaveis de ambiente (ADMIN_EMAIL / ADMIN_PASSWORD).
 * Nunca por INSERT com senha dentro de migracao versionada (Mapa, secao 2).
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private static final Pattern PASSWORD_RULE = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,72}$");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminInitializer(UserRepository users,
                            PasswordEncoder encoder,
                            @Value("${app.admin.email:}") String email,
                            @Value("${app.admin.password:}") String password,
                            @Value("${app.admin.full-name:Administrador}") String fullName) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            log.info("ADMIN_EMAIL nao definido: nenhum ADMIN inicial sera criado.");
            return;
        }
        if (password == null || !PASSWORD_RULE.matcher(password).matches()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD ausente ou fora da regra (8 a 72 caracteres, 1 maiuscula e 1 numero).");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalized)) {
            log.info("ADMIN inicial ja existe ({}).", normalized);
            return;
        }
        users.save(new User(fullName, normalized, encoder.encode(password), Role.ADMIN));
        log.info("ADMIN inicial criado ({}).", normalized);
    }
}
