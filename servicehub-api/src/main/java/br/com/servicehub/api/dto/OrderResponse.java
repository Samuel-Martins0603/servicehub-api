package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.ServiceRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long serviceId,
        String serviceTitle,
        Long clientId,
        String clientName,
        Long providerId,
        RequestStatus status,
        BigDecimal totalPrice,
        LocalDateTime scheduledAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(ServiceRequest o) {
        return new OrderResponse(o.getId(), o.getService().getId(), o.getService().getTitle(),
                o.getClient().getId(), o.getClient().getFullName(), o.getService().getProvider().getId(),
                o.getStatus(), o.getTotalPrice(), o.getScheduledAt(), o.getNotes(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
