package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.OrderCreateRequest;
import br.com.servicehub.api.dto.OrderResponse;
import br.com.servicehub.api.dto.ReviewCreateRequest;
import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.dto.StatusUpdateRequest;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.security.AuthUser;
import br.com.servicehub.api.service.OrderService;
import br.com.servicehub.api.service.ReviewService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/service-requests")
@Tag(name = "Ordens de serviço", description = "Contratação, ciclo de vida (máquina de estados) e avaliação")
public class ServiceRequestController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    public ServiceRequestController(OrderService orderService, ReviewService reviewService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT','PROVIDER')")
    @Operation(summary = "Abre uma ordem de serviço (CLIENT ou PROVIDER)",
            description = "Nasce PENDING com totalPrice congelado (RN07). ADMIN não abre ordens (403).")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request,
                                                @AuthenticationPrincipal AuthUser actor) {
        OrderResponse created = orderService.create(request, actor);
        return ResponseEntity.created(URI.create("/api/v1/service-requests/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista as minhas ordens (ADMIN vê todas)",
            description = "Filtros: status e role=client|provider.")
    @ProblemResponses({400, 401, 500})
    public PagedModel<OrderResponse> list(
            @AuthenticationPrincipal AuthUser actor,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(orderService.list(actor, status, role, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ordem (cliente, prestador do serviço ou ADMIN)")
    @ProblemResponses({401, 403, 404, 500})
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser actor) {
        return orderService.get(id, actor);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Muda o status da ordem",
            description = "Transições da seção 4 do Mapa. Qualquer outra combinação responde 409 invalid-transition.")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                      @AuthenticationPrincipal AuthUser actor) {
        return orderService.changeStatus(id, request.status(), actor);
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Avalia uma ordem concluída (cliente da ordem)",
            description = "Nota de 1 a 5. Só após COMPLETED e uma única vez (RN09).")
    @ProblemResponses({400, 401, 403, 404, 409, 500})
    public ResponseEntity<ReviewResponse> review(@PathVariable Long id, @Valid @RequestBody ReviewCreateRequest request,
                                                 @AuthenticationPrincipal AuthUser actor) {
        ReviewResponse created = reviewService.create(id, request, actor);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + created.id())).body(created);
    }
}
