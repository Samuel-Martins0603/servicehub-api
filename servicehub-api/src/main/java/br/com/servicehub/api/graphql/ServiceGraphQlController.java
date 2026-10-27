package br.com.servicehub.api.graphql;

import br.com.servicehub.api.dto.ServiceResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.security.CurrentUser;
import br.com.servicehub.api.service.ServiceItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

/**
 * Endpoint alternativo GraphQL (POST /graphql), somente leitura do catalogo publico.
 * Reaproveita a MESMA camada de servico do REST, entao as regras (RN12: inativo some) valem igual.
 */
@Controller
public class ServiceGraphQlController {

    private static final int MAX_PAGE_SIZE = 100;

    /** Visao paginada devolvida pela query "services". */
    public record ServicePageView(List<ServiceResponse> content, int page, int size,
                                  long totalElements, int totalPages) {
    }

    private final ServiceItemService serviceItemService;

    public ServiceGraphQlController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @QueryMapping
    public ServicePageView services(@Argument Integer page, @Argument Integer size,
                                    @Argument ServiceCategory category, @Argument Long providerId,
                                    @Argument BigDecimal minPrice, @Argument BigDecimal maxPrice,
                                    @Argument String q) {
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ServiceResponse> result = serviceItemService.list(category, providerId, minPrice, maxPrice, q, pageable);
        return new ServicePageView(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @QueryMapping
    public ServiceResponse service(@Argument Long id) {
        try {
            return serviceItemService.get(id, CurrentUser.get());
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw ex;
        }
    }
}
