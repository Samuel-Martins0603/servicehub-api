package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.dto.UserUpdateRequest;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ServiceItemRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository users;
    @Mock ServiceItemRepository services;
    @Mock ServiceRequestRepository orders;
    @InjectMocks UserService userService;

    private final User provider = TestData.user(1L, "Maria Silva", Role.PROVIDER);
    private final User client = TestData.user(2L, "Joao Pereira", Role.CLIENT);
    private final User stranger = TestData.user(3L, "Carlos Souza", Role.CLIENT);
    private final User admin = TestData.user(4L, "Admin Master", Role.ADMIN);

    // ---------------- RN13 ----------------

    @Test
    @DisplayName("RN13: uma pessoa só vê o próprio perfil (403 para o de outra)")
    void rn13_cannotReadSomeoneElsesProfile() {
        TestData.assertApiError(() -> userService.get(2L, TestData.auth(stranger)), HttpStatus.FORBIDDEN, "forbidden");
    }

    @Test
    @DisplayName("RN13: o próprio usuário e o ADMIN veem o perfil")
    void rn13_selfAndAdminCanReadProfile() {
        when(users.findById(2L)).thenReturn(Optional.of(client));

        assertThat(userService.get(2L, TestData.auth(client)).id()).isEqualTo(2L);
        assertThat(userService.get(2L, TestData.auth(admin)).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("RN13: só o próprio usuário (ou ADMIN) edita o perfil; só nome, telefone, bio e avatar")
    void rn13_updateIsLimitedToSelfOrAdmin() {
        TestData.assertApiError(
                () -> userService.update(2L, new UserUpdateRequest("Hacker", null, null, null), TestData.auth(stranger)),
                HttpStatus.FORBIDDEN, "forbidden");

        when(users.findById(2L)).thenReturn(Optional.of(client));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserResponse updated = userService.update(2L,
                new UserUpdateRequest("Joao P. Pereira", "62999990000", "<b>Eletricista</b>", null), TestData.auth(client));

        assertThat(updated.fullName()).isEqualTo("Joao P. Pereira");
        assertThat(updated.role()).isEqualTo(Role.CLIENT);
        assertThat(updated.bio()).isEqualTo("&lt;b&gt;Eletricista&lt;/b&gt;");
    }

    // ---------------- RN10 e RN11 ----------------

    @Test
    @DisplayName("RN10: usuário com ordens ativas não pode ser excluído (409)")
    void rn10_cannotDeleteUserWithActiveOrders() {
        when(users.findById(1L)).thenReturn(Optional.of(provider));
        when(orders.existsActiveForUser(eq(1L), anyCollection())).thenReturn(true);

        TestData.assertApiError(() -> userService.delete(1L, TestData.auth(admin)), HttpStatus.CONFLICT, "business-rule");
        assertThat(provider.isActive()).isTrue();
    }

    @Test
    @DisplayName("RN10: o ADMIN não exclui a própria conta (409)")
    void rn10_adminCannotDeleteOwnAccount() {
        when(users.findById(4L)).thenReturn(Optional.of(admin));

        TestData.assertApiError(() -> userService.delete(4L, TestData.auth(admin)), HttpStatus.CONFLICT, "business-rule");
    }

    @Test
    @DisplayName("RN10: o último ADMIN ativo não pode ser excluído (409)")
    void rn10_lastAdminCannotBeDeleted() {
        User otherAdmin = TestData.user(5L, "Segundo Admin", Role.ADMIN);
        when(users.findById(5L)).thenReturn(Optional.of(otherAdmin));
        when(users.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        TestData.assertApiError(() -> userService.delete(5L, TestData.auth(admin)), HttpStatus.CONFLICT, "business-rule");
        assertThat(otherAdmin.isActive()).isTrue();
    }

    @Test
    @DisplayName("RN11: desativar um usuário desativa os serviços dele (exclusão lógica)")
    void rn11_deactivatingUserDeactivatesHisServices() {
        ServiceItem s1 = TestData.service(10L, provider, "150.00");
        ServiceItem s2 = TestData.service(11L, provider, "80.00");
        when(users.findById(1L)).thenReturn(Optional.of(provider));
        when(orders.existsActiveForUser(eq(1L), anyCollection())).thenReturn(false);
        when(services.findByProviderId(1L)).thenReturn(List.of(s1, s2));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.delete(1L, TestData.auth(admin));

        assertThat(provider.isActive()).isFalse();
        assertThat(s1.isActive()).isFalse();
        assertThat(s2.isActive()).isFalse();
    }

    @Test
    @DisplayName("RN03: o ADMIN não altera o próprio papel (409)")
    void rn03_adminCannotChangeOwnRole() {
        when(users.findById(4L)).thenReturn(Optional.of(admin));

        TestData.assertApiError(() -> userService.changeRole(4L, Role.CLIENT, TestData.auth(admin)),
                HttpStatus.CONFLICT, "business-rule");
    }

    @Test
    @DisplayName("RN03: o ADMIN altera o papel de outro usuário")
    void rn03_adminChangesAnotherUsersRole() {
        when(users.findById(2L)).thenReturn(Optional.of(client));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.changeRole(2L, Role.PROVIDER, TestData.auth(admin));

        assertThat(response.role()).isEqualTo(Role.PROVIDER);
    }
}
