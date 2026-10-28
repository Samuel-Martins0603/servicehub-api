package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServiceCreateRequest;
import br.com.servicehub.api.dto.ServiceResponse;
import br.com.servicehub.api.dto.ServiceUpdateRequest;
import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ReviewRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Catalogo de servicos. RN04 (prestador = usuario do token), RN05 (so dono ou ADMIN altera),
 * RN10 (ordens ativas impedem desativar/excluir), RN12 (inativo some e da 404 ao publico).
 */
@Service
public class ServiceItemService {

    private final ServiceItemRepository services;
    private final UserRepository users;
    private final ServiceRequestRepository orders;
    private final ReviewRepository reviews;

    public ServiceItemService(ServiceItemRepository services, UserRepository users,
                              ServiceRequestRepository orders, ReviewRepository reviews) {
        this.services = services;
        this.users = users;
        this.orders = orders;
        this.reviews = reviews;
    }

    /** RN12: a listagem publica so tem servicos ativos. */
    @Transactional(readOnly = true)
    public Page<ServiceResponse> list(ServiceCategory category, Long providerId, BigDecimal minPrice,
                                      BigDecimal maxPrice, String q, Pageable pageable) {
        Pages.check(pageable, "createdAt", "updatedAt", "price", "title", "id");
        Specification<ServiceItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (providerId != null) {
                predicates.add(cb.equal(root.get("provider").get("id"), providerId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), maxPrice));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("title")), pattern, '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return services.findAll(spec, pageable).map(ServiceResponse::from);
    }

    /** actor pode ser null (visitante anonimo). */
    @Transactional(readOnly = true)
    public ServiceResponse get(Long id, AuthUser actor) {
        return ServiceResponse.from(findVisible(id, actor));
    }

    /** RN04: o prestador e o usuario do token, nunca um id vindo do corpo. */
    @Transactional
    public ServiceResponse create(ServiceCreateRequest request, AuthUser actor) {
        if (actor.role() != Role.PROVIDER) {
            throw ApiException.forbidden("Somente PROVIDER publica serviços.");
        }
        User provider = users.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("Usuário do token não existe mais."));
        ServiceItem item = new ServiceItem(request.title().trim(), TextSanitizer.clean(request.description()),
                request.price(), request.category(), provider);
        return ServiceResponse.from(services.save(item));
    }

    @Transactional
    public ServiceResponse update(Long id, ServiceUpdateRequest request, AuthUser actor) {
        ServiceItem item = findForWrite(id, actor);
        if (request.active() != null) {
            if (!request.active() && item.isActive()
                    && orders.existsActiveForService(id, RequestStatus.activeStatuses())) {
                throw ApiException.businessRule("O serviço possui ordens ativas e não pode ser desativado.");
            }
            if (request.active() && !item.getProvider().isActive()) {
                throw ApiException.businessRule("O prestador está inativo; o serviço não pode ser reativado.");
            }
            item.setActive(request.active());
        }
        item.setTitle(request.title().trim());
        item.setDescription(TextSanitizer.clean(request.description()));
        item.setPrice(request.price());
        item.setCategory(request.category());
        return ServiceResponse.from(services.save(item));
    }

    /** Exclusao logica (is_active=false), barrada por ordens ativas (RN10). */
    @Transactional
    public void delete(Long id, AuthUser actor) {
        ServiceItem item = findForWrite(id, actor);
        if (orders.existsActiveForService(id, RequestStatus.activeStatuses())) {
            throw ApiException.businessRule("O serviço possui ordens ativas e não pode ser excluído.");
        }
        item.setActive(false);
        services.save(item);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> reviewsOf(Long serviceId, AuthUser actor, Pageable pageable) {
        findVisible(serviceId, actor);
        Pages.check(pageable, "createdAt", "rating", "id");
        return reviews.findByRequestServiceId(serviceId, pageable).map(ReviewResponse::from);
    }

    // ---------- helpers ----------

    /** RN12: servico inativo responde 404 ao publico; dono e ADMIN continuam vendo. */
    private ServiceItem findVisible(Long id, AuthUser actor) {
        ServiceItem item = services.findById(id).orElseThrow(() -> ApiException.notFound("Serviço não encontrado."));
        if (!item.isActive() && !isOwnerOrAdmin(item, actor)) {
            throw ApiException.notFound("Serviço não encontrado.");
        }
        return item;
    }

    /** RN05: so o dono ou um ADMIN altera/desativa (403). Servico inativo de outra pessoa continua 404. */
    private ServiceItem findForWrite(Long id, AuthUser actor) {
        ServiceItem item = services.findById(id).orElseThrow(() -> ApiException.notFound("Serviço não encontrado."));
        if (!isOwnerOrAdmin(item, actor)) {
            if (!item.isActive()) {
                throw ApiException.notFound("Serviço não encontrado.");
            }
            throw ApiException.forbidden("Somente o dono do serviço ou um ADMIN pode alterá-lo.");
        }
        return item;
    }

    private boolean isOwnerOrAdmin(ServiceItem item, AuthUser actor) {
        return actor != null && (actor.isAdmin() || item.getProvider().getId().equals(actor.id()));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
