package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.dto.UserUpdateRequest;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ServiceItemRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usuarios. RN13: cada pessoa so ve/edita o proprio perfil (ADMIN ve tudo).
 * RN10/RN11: excluir ou desativar e SEMPRE logico, barrado por ordens ativas, e desativa os servicos do usuario.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final ServiceItemRepository services;
    private final ServiceRequestRepository orders;

    public UserService(UserRepository users, ServiceItemRepository services, ServiceRequestRepository orders) {
        this.users = users;
        this.services = services;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser actor) {
        return UserResponse.from(find(actor.id()));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(Pages.check(pageable, "createdAt", "updatedAt", "fullName", "email", "id"))
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id, AuthUser actor) {
        requireSelfOrAdmin(id, actor);
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, AuthUser actor) {
        requireSelfOrAdmin(id, actor);
        User user = find(id);
        user.setFullName(request.fullName().trim());
        user.setPhone(TextSanitizer.clean(request.phone()));
        user.setBio(TextSanitizer.clean(request.bio()));
        user.setAvatarUrl(TextSanitizer.clean(request.avatarUrl()));
        return UserResponse.from(users.save(user));
    }

    /** DELETE /users/{id}: exclusao logica, com as mesmas barreiras de desativar. */
    @Transactional
    public void delete(Long id, AuthUser actor) {
        deactivate(id, actor);
    }

    @Transactional
    public UserResponse changeRole(Long id, Role newRole, AuthUser actor) {
        User user = find(id);
        if (user.getId().equals(actor.id())) {
            throw ApiException.businessRule("Você não pode alterar o próprio papel.");
        }
        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN && isLastActiveAdmin(user)) {
            throw ApiException.businessRule("Não é possível rebaixar o último ADMIN ativo.");
        }
        user.setRole(newRole);
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public UserResponse changeActive(Long id, boolean active, AuthUser actor) {
        if (!active) {
            return UserResponse.from(deactivate(id, actor));
        }
        User user = find(id);
        user.setActive(true);
        return UserResponse.from(users.save(user));
    }

    /** Barreiras (RN10): propria conta, ultimo ADMIN e ordens PENDING/ACCEPTED/IN_PROGRESS dao 409. */
    private User deactivate(Long id, AuthUser actor) {
        User user = find(id);
        if (user.getId().equals(actor.id())) {
            throw ApiException.businessRule("Você não pode desativar ou excluir a própria conta.");
        }
        if (user.getRole() == Role.ADMIN && isLastActiveAdmin(user)) {
            throw ApiException.businessRule("Não é possível desativar ou excluir o último ADMIN ativo.");
        }
        if (orders.existsActiveForUser(user.getId(), RequestStatus.activeStatuses())) {
            throw ApiException.businessRule("O usuário possui ordens de serviço ativas.");
        }
        user.setActive(false);
        // RN11: desativar o usuario desativa os servicos dele.
        for (ServiceItem service : services.findByProviderId(user.getId())) {
            service.setActive(false);
        }
        return users.save(user);
    }

    private boolean isLastActiveAdmin(User user) {
        return user.isActive() && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1;
    }

    private void requireSelfOrAdmin(Long id, AuthUser actor) {
        if (!actor.isAdmin() && !actor.id().equals(id)) {
            throw ApiException.forbidden("Você só pode acessar o seu próprio perfil.");
        }
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    }
}
