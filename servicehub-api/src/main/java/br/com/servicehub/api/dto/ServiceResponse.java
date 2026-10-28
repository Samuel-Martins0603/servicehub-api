package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.model.ServiceItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        ServiceCategory category,
        boolean active,
        Long providerId,
        String providerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServiceResponse from(ServiceItem s) {
        return new ServiceResponse(s.getId(), s.getTitle(), s.getDescription(), s.getPrice(), s.getCategory(),
                s.isActive(), s.getProvider().getId(), s.getProvider().getFullName(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
