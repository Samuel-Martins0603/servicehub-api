package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.dto.ServiceCreateRequest;
import br.com.servicehub.api.dto.ServiceResponse;
import br.com.servicehub.api.dto.ServiceUpdateRequest;
import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.security.AuthUser;
import br.com.servicehub.api.service.ServiceItemService;
import br.com.servicehub.api.web.ProblemResponses;
import br.com.servicehub.api.web.PublicEndpoint;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Serviços", description = "Catálogo de serviços publicados pelos prestadores")
public class ServiceController {

    private final ServiceItemService serviceItemService;

    public ServiceController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @PublicEndpoint
    @GetMapping
    @Operation(summary = "Lista serviços ativos (público)",
            description = "Filtros: category, providerId, minPrice, maxPrice, q (busca no título, sem diferenciar maiúsculas).")
    @ProblemResponses({400, 500})
    public PagedModel<ServiceResponse> list(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(serviceItemService.list(category, providerId, minPrice, maxPrice, q, pageable));
    }

    @PublicEndpoint
    @GetMapping("/{id}")
    @Operation(summary = "Busca um serviço (público)",
            description = "Serviço inativo responde 404 ao público; o dono e o ADMIN continuam vendo (RN12).")
    @ProblemResponses({404, 500})
    public ServiceResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        return serviceItemService.get(id, actor);
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Publica um serviço (PROVIDER)",
            description = "O prestador é o usuário do token (RN04). 201 com Location.")
    @ProblemResponses({400, 401, 403, 500})
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceCreateRequest request,
                                                  @AuthenticationPrincipal AuthUser actor) {
        ServiceResponse created = serviceItemService.create(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/services/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um serviço (dono ou ADMIN)",
            description = "Aceita os mesmos campos do POST mais active. 409 ao desativar com ordens ativas (RN10).")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public ServiceResponse update(@PathVariable Long id, @Valid @RequestBody ServiceUpdateRequest request,
                                  @AuthenticationPrincipal AuthUser actor) {
        return serviceItemService.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclusão lógica de um serviço (dono ou ADMIN)",
            description = "409 se houver ordens PENDING, ACCEPTED ou IN_PROGRESS (RN10).")
    @ProblemResponses({401, 403, 404, 409, 500})
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        serviceItemService.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PublicEndpoint
    @GetMapping("/{id}/reviews")
    @Operation(summary = "Lista as avaliações de um serviço (público)")
    @ProblemResponses({400, 404, 500})
    public PagedModel<ReviewResponse> reviews(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser actor,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(serviceItemService.reviewsOf(id, actor, pageable));
    }
}
