package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.ActiveUpdateRequest;
import br.com.servicehub.api.dto.RoleUpdateRequest;
import br.com.servicehub.api.dto.UserResponse;
import br.com.servicehub.api.dto.UserUpdateRequest;
import br.com.servicehub.api.security.AuthUser;
import br.com.servicehub.api.service.UserService;
import br.com.servicehub.api.web.ProblemResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Gestão de usuários (perfil próprio ou ADMIN)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista usuários (ADMIN)")
    @ProblemResponses({400, 401, 403, 500})
    public PagedModel<UserResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(userService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário (o próprio ou ADMIN)")
    @ProblemResponses({401, 403, 404, 500})
    public UserResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        return userService.get(id, actor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, telefone, bio e avatar (o próprio ou ADMIN)")
    @ProblemResponses({400, 401, 403, 404, 500})
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal AuthUser actor) {
        return userService.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclusão lógica de usuário (ADMIN)",
            description = "Desativa o usuário e os serviços dele. 409: ordens ativas, própria conta ou último ADMIN.")
    @ProblemResponses({401, 403, 404, 409, 500})
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        userService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Altera o papel de um usuário (ADMIN)")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request,
                                   @AuthenticationPrincipal AuthUser actor) {
        return userService.changeRole(id, request.role(), actor);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativa ou desativa um usuário (ADMIN)")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public UserResponse changeActive(@PathVariable Long id, @Valid @RequestBody ActiveUpdateRequest request,
                                     @AuthenticationPrincipal AuthUser actor) {
        return userService.changeActive(id, request.active(), actor);
    }
}
