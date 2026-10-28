package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.OrderCreateRequest;
import br.com.servicehub.api.dto.OrderResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.ServiceRequest;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ServiceItemRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ordens de servico. RN06 (quem pode contratar), RN07 (preco congelado), RN08 (maquina de estados),
 * RN13 (so participantes veem a ordem; ADMIN ve todas).
 */
@Service
public class OrderService {

    private final ServiceRequestRepository orders;
    private final ServiceItemRepository services;
    private final UserRepository users;

    public OrderService(ServiceRequestRepository orders, ServiceItemRepository services, UserRepository users) {
        this.orders = orders;
        this.services = services;
        this.users = users;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request, AuthUser actor) {
        // RN06: CLIENT e PROVIDER abrem ordens; ADMIN nao.
        if (actor.role() == Role.ADMIN) {
            throw ApiException.forbidden("ADMIN não abre ordens de serviço.");
        }
        ServiceItem service = services.findById(request.serviceId())
                .orElseThrow(() -> ApiException.notFound("Serviço não encontrado."));
        // RN06: o servico precisa estar ativo e ninguem contrata o proprio servico.
        if (!service.isActive()) {
            throw ApiException.businessRule("O serviço está inativo.");
        }
        if (service.getProvider().getId().equals(actor.id())) {
            throw ApiException.businessRule("Não é possível contratar o próprio serviço.");
        }
        User client = users.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("Usuário do token não existe mais."));
        // RN07: o construtor copia services.price para totalPrice (sem setter depois).
        ServiceRequest order = new ServiceRequest(service, client, request.scheduledAt(),
                TextSanitizer.clean(request.notes()));
        return OrderResponse.from(orders.save(order));
    }

    /** ADMIN ve todas; os demais so as ordens em que participam (cliente ou prestador). */
    @Transactional(readOnly = true)
    public Page<OrderResponse> list(AuthUser actor, RequestStatus status, String roleFilter, Pageable pageable) {
        Pages.check(pageable, "createdAt", "updatedAt", "status", "totalPrice", "scheduledAt", "id");
        String role = roleFilter == null ? null : roleFilter.trim().toLowerCase(Locale.ROOT);
        if (role != null && !role.isEmpty() && !role.equals("client") && !role.equals("provider")) {
            throw ApiException.badRequest("role", "deve ser client ou provider");
        }
        Specification<ServiceRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (!actor.isAdmin()) {
                Predicate asClient = cb.equal(root.get("client").get("id"), actor.id());
                Predicate asProvider = cb.equal(root.get("service").get("provider").get("id"), actor.id());
                if ("client".equals(role)) {
                    predicates.add(asClient);
                } else if ("provider".equals(role)) {
                    predicates.add(asProvider);
                } else {
                    predicates.add(cb.or(asClient, asProvider));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orders.findAll(spec, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, AuthUser actor) {
        ServiceRequest order = find(id);
        requireParticipant(order, actor);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse changeStatus(Long id, RequestStatus target, AuthUser actor) {
        ServiceRequest order = find(id);
        // 403 vem ANTES de avaliar a transicao (Mapa, secao 4).
        requireParticipant(order, actor);

        RequestStatus current = order.getStatus();
        // RN08: so as transicoes da tabela; qualquer outra e 409 invalid-transition.
        if (!current.canTransitionTo(target)) {
            throw ApiException.invalidTransition("Não é possível mudar de " + current + " para " + target + ".");
        }

        boolean isClient = order.getClient().getId().equals(actor.id());
        boolean isProvider = order.getService().getProvider().getId().equals(actor.id());
        boolean allowed = target == RequestStatus.CANCELLED
                ? (isClient || isProvider || actor.isAdmin())   // quem abriu ou o prestador dono
                : (isProvider || actor.isAdmin());              // aceitar, iniciar, concluir: prestador dono
        if (!allowed) {
            throw ApiException.forbidden("Você não pode mudar a ordem para " + target + ".");
        }
        order.setStatus(target);
        return OrderResponse.from(orders.save(order));
    }

    // ---------- helpers ----------

    private ServiceRequest find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Ordem de serviço não encontrada."));
    }

    /** RN13: cliente da ordem, prestador do servico ou ADMIN. */
    private void requireParticipant(ServiceRequest order, AuthUser actor) {
        boolean participant = actor.isAdmin()
                || order.getClient().getId().equals(actor.id())
                || order.getService().getProvider().getId().equals(actor.id());
        if (!participant) {
            throw ApiException.forbidden("Você não participa desta ordem de serviço.");
        }
    }
}
